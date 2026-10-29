const app = document.querySelector("#app");
const catalog = window.EXAM_CATALOG || [];
const SUBJECTS = window.SUBJECTS_BY_SESSION || {};
const letters = ["①", "②", "③", "④", "⑤"];
const cache = new Map();
let route = { name: "home" };
let quiz = null;

document.addEventListener("click", event => {
  const target = event.target.closest("[data-action], [data-home]");
  if (!target || target.disabled) return;
  if (target.dataset.home !== undefined) return navigate({ name: "home" });
  const action = target.dataset.action;
  if (action === "back") return navigate(route.back || { name: "home" });
  if (action === "exam") return navigate({ name: "subjects", examId: target.dataset.examId, back: { name: "exams" } });
  if (action === "subject") return startOriginal(target.dataset.examId, Number(target.dataset.subjectIndex));
  if (action === "mock") return navigate({ name: "mock", back: { name: "home" } });
  if (action === "mock-start") return startMock(Number(target.dataset.year), Number(target.dataset.subjectIndex));
  if (action === "submit") return submitQuiz();
  if (action === "next") return moveQuestion(1);
  if (action === "prev") return moveQuestion(-1);
  if (action === "omr") return renderOmr();
  if (action === "answer") return chooseAnswer(Number(target.dataset.index));
  if (action === "jump") { quiz.index = Number(target.dataset.index); return renderQuiz(); }
});

function navigate(next) { route = next; render(); window.scrollTo({ top: 0, behavior: "instant" }); }
function escape(value) { const node = document.createElement("span"); node.textContent = value ?? ""; return node.innerHTML; }
function button(label, action, extra = "") { return `<button class="button ${extra}" type="button" data-action="${action}">${label}</button>`; }
function back(backRoute) { route.back = backRoute; return '<button class="back" type="button" data-action="back">← 돌아가기</button>'; }
function examTitle(exam) { return `${exam.year}년 제${exam.round}회 ${exam.session}교시`; }
function render() {
  if (route.name === "home") return renderHome();
  if (route.name === "exams") return renderExams();
  if (route.name === "subjects") return renderSubjects();
  if (route.name === "mock") return renderMock();
  if (route.name === "quiz") return renderQuiz();
  if (route.name === "result") return renderResult();
}

function renderHome() {
  app.innerHTML = `<section class="hero"><p class="eyebrow">문제 풀이</p><h1>기출의 흐름대로,<br>내 속도로.</h1><p>청소년상담사 3급 기출을 과목별로 풉니다.</p></section>
    <section class="stack">
      <button class="choice" type="button" data-action="show-exams"><span><strong>기출문제 풀기</strong><span>연도 · 교시 · 과목 단위 CBT</span></span><b class="arrow">→</b></button>
      <button class="choice" type="button" data-action="mock"><span><strong>모의고사</strong><span>한 회차 전체를 시험처럼 풀기</span></span><b class="arrow">→</b></button>
    </section>`;
  app.querySelector('[data-action="show-exams"]').onclick = () => navigate({ name: "exams", back: { name: "home" } });
}

function renderExams() {
  app.innerHTML = `${back({ name: "home" })}<p class="eyebrow">기출문제</p><h1>회차를 선택하세요</h1><div class="stack">${catalog.slice().sort((a, b) => b.year - a.year || b.session - a.session).map(exam => `<button class="choice" type="button" data-action="exam" data-exam-id="${exam.id}"><span><strong>${examTitle(exam)}</strong><span>${exam.session === 1 ? "필수 4과목 · 100문항" : "필수 1과목 + 선택 2과목 · 75문항"}</span></span><b class="arrow">→</b></button>`).join("")}</div>`;
}

async function renderSubjects() {
  const exam = findExam(route.examId);
  app.innerHTML = `${back(route.back || { name: "exams" })}<div class="loading">문제를 준비하고 있습니다…</div>`;
  try {
    const questions = await loadExam(exam);
    const subjects = splitSubjects(exam, questions);
    app.innerHTML = `${back(route.back || { name: "exams" })}<p class="eyebrow">${examTitle(exam)}</p><h1>과목을 선택하세요</h1><div class="stack">${subjects.map((subject, index) => `<button class="choice" type="button" data-action="subject" data-exam-id="${exam.id}" data-subject-index="${index}"><span><strong>${escape(subject.name)}</strong><span>${subject.questions.length}문항</span></span><b class="arrow">→</b></button>`).join("")}</div>`;
  } catch (error) { renderFailure(error, route.back || { name: "exams" }); }
}

function renderFailure(error, backRoute) { app.innerHTML = `${back(backRoute)}<div class="card"><h2>문제를 불러오지 못했습니다</h2><p>${escape(error.message || "네트워크 연결을 확인해주세요.")}</p></div>`; }
function findExam(id) { const exam = catalog.find(item => item.id === id); if (!exam) throw new Error("시험 정보를 찾지 못했습니다."); return exam; }

async function loadExam(exam) {
  if (cache.has(exam.id)) return cache.get(exam.id);
  const response = await fetch(exam.url);
  if (!response.ok) throw new Error("기출 데이터를 불러오지 못했습니다.");
  if (exam.url.endsWith(".json")) {
    const values = (await response.json()).map(row => ({ ...row, id: `${exam.id}-${row.number}`, answer: row.answers[0] - 1, answers: row.answers.map(answer => answer - 1), explanation: row.explanation || "", image: row.image || "" }));
    if (!values.length || values.some(question => !question.question || question.options?.length !== 5 || !question.answers.length)) throw new Error("기출 데이터 형식이 올바르지 않습니다.");
    cache.set(exam.id, values); return values;
  }
  const rows = parseCsv(await response.text());
  const [headers, ...data] = rows;
  const values = data.map(row => Object.fromEntries(headers.map((header, index) => [header.trim(), row[index]?.trim() || ""]))).map(row => {
    const answers = String(row["정답"]).split(/[,/\s]+/).map(Number).filter(answer => answer >= 1 && answer <= 5).map(answer => answer - 1);
    return {
      id: `${exam.id}-${row["문제번호"]}`,
      number: Number(row["문제번호"]), question: row["문제내용"],
      options: [row["보기1"], row["보기2"], row["보기3"], row["보기4"], row["보기5"]],
      answer: answers[0], answers, explanation: row["해설"], image: row["이미지"],
    };
  }).filter(question => question.question && question.options.every(Boolean) && question.answers.length);
  if (!values.length) throw new Error("사용 가능한 문항이 없습니다.");
  cache.set(exam.id, values); return values;
}

function splitSubjects(exam, questions) { return (SUBJECTS[exam.session] || []).map((name, index) => ({ name, questions: questions.slice(index * 25, index * 25 + 25) })).filter(subject => subject.questions.length); }
function parseCsv(text) { const rows = []; let row = [], value = "", quoted = false; for (let i = 0; i < text.length; i += 1) { const char = text[i]; if (char === '"') { if (quoted && text[i + 1] === '"') { value += char; i += 1; } else quoted = !quoted; } else if (char === "," && !quoted) { row.push(value); value = ""; } else if ((char === "\n" || char === "\r") && !quoted) { if (char === "\r" && text[i + 1] === "\n") i += 1; row.push(value); if (row.some(Boolean)) rows.push(row); row = []; value = ""; } else value += char; } row.push(value); if (row.some(Boolean)) rows.push(row); return rows; }

async function startOriginal(examId, subjectIndex) { const exam = findExam(examId); const subject = splitSubjects(exam, await loadExam(exam))[subjectIndex]; startQuiz({ id: `original-${exam.id}-${subjectIndex}`, title: `${examTitle(exam)} · ${subject.name}`, source: "기출", questions: subject.questions, back: { name: "subjects", examId, back: { name: "exams" } } }); }
function renderMock() {
  const years = [...new Set(catalog.map(exam => exam.year))].filter(year => catalog.some(exam => exam.year === year && exam.session === 1) && catalog.some(exam => exam.year === year && exam.session === 2)).sort((a, b) => b - a);
  app.innerHTML = `${back(route.back || { name: "home" })}<p class="eyebrow">기출 모의고사</p><h1>회차를 선택하세요</h1><p class="page-copy">1교시 100문항과 2교시 필수 과목·선택 과목 25문항을 연속으로 풉니다.</p><div class="stack">${years.map(year => `<section class="card"><h2>${year}년 제${year - 2001}회</h2><p>2교시 선택 과목을 고르세요.</p><div class="button-row">${[1, 2].map(index => button(SUBJECTS[2][index], "mock-start", "secondary").replace('data-action="mock-start"', `data-action="mock-start" data-year="${year}" data-subject-index="${index}"`)).join("")}</div></section>`).join("")}</div>`;
}
async function startMock(year, subjectIndex) {
  const first = catalog.find(exam => exam.year === year && exam.session === 1); const second = catalog.find(exam => exam.year === year && exam.session === 2);
  if (!first || !second) return;
  app.innerHTML = '<div class="loading">모의고사를 준비하고 있습니다…</div>';
  try {
    const firstSubjects = splitSubjects(first, await loadExam(first)); const secondSubjects = splitSubjects(second, await loadExam(second));
    const choice = secondSubjects[subjectIndex];
    startQuiz({ id: `mock-${year}-${subjectIndex}`, title: `${year}년 제${first.round}회 모의고사 · ${choice.name} 선택`, source: "모의고사", questions: [...firstSubjects.flatMap(subject => subject.questions), ...secondSubjects[0].questions, ...choice.questions], back: { name: "mock", back: { name: "home" } } });
  } catch (error) { renderFailure(error, { name: "mock", back: { name: "home" } }); }
}
function startQuiz(set) { quiz = { ...set, answers: Array(set.questions.length).fill(null), index: 0 }; navigate({ name: "quiz", back: set.back }); }
function chooseAnswer(index) { quiz.answers[quiz.index] = index; renderQuiz(); }
function moveQuestion(offset) { quiz.index = Math.max(0, Math.min(quiz.questions.length - 1, quiz.index + offset)); renderQuiz(); }

function renderQuiz() {
  const question = quiz.questions[quiz.index]; const answered = quiz.answers.filter(answer => answer !== null).length;
  app.innerHTML = `${back(route.back || { name: "home" })}<div class="quiz-head"><span>${escape(quiz.title)}</span><span>${quiz.index + 1} / ${quiz.questions.length}</span></div><div class="progress"><div style="width:${((quiz.index + 1) / quiz.questions.length) * 100}%"></div></div><section class="card"><p class="question">${escape(`Q${quiz.index + 1}. ${question.question}`)}</p>${question.image ? `<img class="question-image" src="${escape(question.image)}" alt="문제 이미지" />` : ""}<div class="options">${question.options.map((option, index) => `<button class="option ${quiz.answers[quiz.index] === index ? "selected" : ""}" type="button" data-action="answer" data-index="${index}">${letters[index]} ${escape(option)}</button>`).join("")}</div></section><section class="quiz-controls"><p>${answered}/${quiz.questions.length} 문항 선택</p><div class="quiz-actions">${button("← 이전", "prev", "secondary")} ${button("문항표", "omr", "secondary")} ${button("다음 →", "next", "secondary")} ${button("채점하기", "submit", "secondary")}</div></section>`;
  app.querySelector('[data-action="prev"]').disabled = quiz.index === 0; app.querySelector('[data-action="next"]').disabled = quiz.index === quiz.questions.length - 1;
}

function renderOmr() { const html = `${back({ name: "quiz", back: route.back })}<p class="eyebrow">${escape(quiz.title)}</p><h1>문항표</h1><section class="card"><div class="omr">${quiz.questions.map((_, index) => `<button class="${quiz.answers[index] !== null ? "marked" : ""} ${quiz.index === index ? "current" : ""}" type="button" data-action="jump" data-index="${index}">${index + 1}</button>`).join("")}</div><div class="button-row">${button("채점하기", "submit")}</div></section>`; app.innerHTML = html; }

function correctAnswers(question) { return question.answers?.length ? question.answers : [question.answer]; }
function originLabel(question) { return question.explanation?.match(/\n\n\[원문: (.+)\]$/)?.[1] || ""; }
function displayExplanation(question) { return (question.explanation || "").replace(/\n\n\[원문: .+\]$/, ""); }
function submitQuiz() { const results = quiz.questions.map((question, index) => ({ question, answer: quiz.answers[index], correct: correctAnswers(question).includes(quiz.answers[index]) })); const correct = results.filter(result => result.correct).length; route = { name: "result", back: quiz.back }; route.results = results; route.correct = correct; renderResult(); }
function renderResult() { const score = Math.round((route.correct / route.results.length) * 100); const status = score >= 60 ? ["good", "목표 점수권입니다."] : score >= 40 ? ["warn", "과목 연습 결과입니다. 오답을 복습해보세요."] : ["bad", "오답 중심으로 다시 풀어보세요."]; app.innerHTML = `${back(route.back)}<section class="card"><p class="eyebrow">${escape(quiz.title)}</p><h1>채점 결과</h1><p class="score">${score}점</p><p class="status ${status[0]}">${status[1]} (${route.correct}/${route.results.length})</p></section><section class="card"><h2>문항별 결과</h2>${route.results.map((result, index) => `<article class="review"><h3>${result.correct ? "✓ 정답" : "✕ 오답"} · ${index + 1}번</h3><div class="result-block"><strong>질문</strong><p>${escape(result.question.question)}</p></div>${originLabel(result.question) ? `<p class="origin">원문 기반: ${escape(originLabel(result.question))}</p>` : ""}<div class="result-block"><strong>정답 보기</strong><p>${correctAnswers(result.question).map(answer => `${letters[answer]} ${escape(result.question.options[answer])}`).join(" 또는 ")}</p></div><div class="result-block"><strong>내 선택</strong><p>${result.answer === null ? "미응시" : `${letters[result.answer]} ${escape(result.question.options[result.answer])}`}</p></div>${displayExplanation(result.question).trim() ? `<details><summary>해설 보기</summary><p>${escape(displayExplanation(result.question))}</p></details>` : ""}</article>`).join("")}</section>`; }

navigate({ name: "home" });
