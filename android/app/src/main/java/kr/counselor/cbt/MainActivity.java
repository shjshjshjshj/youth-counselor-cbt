package kr.counselor.cbt;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
  static final String[] SUBJECTS={"발달심리","집단상담의 기초","심리측정 및 평가","상담이론","학습이론","청소년이해론","청소년수련활동론"};
  final ExecutorService worker=Executors.newSingleThreadExecutor();
  JSONArray exams, questions=new JSONArray();
  LinearLayout page, shell; TextView answeredText;
  static final int CANVAS=0xfff7f7fb, BORDER=0xffd1d5db, BRAND=0xff4338ca, PALE=0xffeef2ff;
  Runnable backAction; boolean result=false;
  interface Job { void run() throws Exception; }
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    try { exams=new JSONArray(readText(getAssets().open("exams.json"))); }
    catch(Exception e){ fatal("기출 데이터를 읽지 못했습니다.");return; }
    if(state!=null && state.containsKey("quiz")) {
      try { questions=new JSONArray(state.getString("quiz")); answers=state.getIntArray("answers");index=state.getInt("index");title=state.getString("title");result=state.getBoolean("result");if(questions.length()>0){if(result)results();else quiz();return;} } catch(Exception ignored){}
    }
    home();
  }
  @Override protected void onSaveInstanceState(Bundle s){ super.onSaveInstanceState(s); if(!busy && (screen==2||screen==3)){s.putString("quiz",questions.toString());s.putIntArray("answers",answers);s.putInt("index",index);s.putString("title",title);s.putBoolean("result",result);} }
  @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
  int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
  void layout(String heading,Runnable back){
    screen=0;backAction=back;
    shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundColor(CANVAS);
    shell.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
    if(back!=null){Button up=control("‹  돌아가기",back);up.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);up.setBackgroundColor(Color.TRANSPARENT);shell.addView(up,new LinearLayout.LayoutParams(-1,dp(52)));}
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(20),dp(16),dp(20),dp(24));scroll.addView(page);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(shell);shell.requestApplyInsets();
    text(heading,26,true);
  }
  android.graphics.drawable.GradientDrawable surface(int fill,int stroke){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(14));d.setStroke(dp(1),stroke);return d;}
  android.graphics.drawable.Drawable interactive(){android.graphics.drawable.StateListDrawable d=new android.graphics.drawable.StateListDrawable();d.addState(new int[]{-android.R.attr.state_enabled},surface(0xffe4e4e7,BORDER));d.addState(new int[]{android.R.attr.state_selected},surface(PALE,BRAND));d.addState(new int[]{android.R.attr.state_checked},surface(PALE,BRAND));d.addState(new int[]{android.R.attr.state_pressed},surface(PALE,BRAND));d.addState(new int[]{},surface(Color.WHITE,BORDER));return new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x224338ca),d,null);}
  TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextColor(Color.BLACK);t.setTextSize(size);t.setLineSpacing(dp(3),1.1f);t.setPadding(0,dp(6),0,dp(12));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);page.addView(t);return t;}
  Button control(String label,Runnable click){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.BLACK);b.setTextSize(16);b.setTypeface(null,android.graphics.Typeface.BOLD);b.setMinHeight(dp(52));b.setMinimumWidth(0);b.setMinWidth(0);b.setPadding(dp(16),dp(12),dp(16),dp(12));b.setStateListAnimator(null);b.setBackgroundTintList(null);b.setBackground(interactive());b.setOnClickListener(v->click.run());return b;}
  Button button(String label,Runnable click){Button b=control(label,click);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);p.bottomMargin=dp(4);page.addView(b,p);return b;}
  void choice(String label,String detail,Runnable click){String heading=label+"   ›";android.text.SpannableString copy=new android.text.SpannableString(heading+"\n"+detail);copy.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),0,heading.length(),0);copy.setSpan(new android.text.style.AbsoluteSizeSpan(18,true),0,heading.length(),0);copy.setSpan(new android.text.style.AbsoluteSizeSpan(14,true),heading.length()+1,copy.length(),0);Button b=button("",click);b.setTypeface(null,android.graphics.Typeface.NORMAL);b.setText(copy);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setLineSpacing(dp(6),1);b.setPadding(dp(20),dp(20),dp(20),dp(20));}
  LinearLayout card(){LinearLayout parent=page,box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(16),dp(16),dp(16));box.setBackground(surface(Color.WHITE,BORDER));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(12);p.bottomMargin=dp(4);parent.addView(box,p);page=box;return parent;}
  void line(){View v=new View(this);v.setBackgroundColor(0xffdddddd);page.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
  Spinner spinner(String label,String[] values){text(label+"  ▾",16,true);Spinner s=new Spinner(this,Spinner.MODE_DIALOG);s.setPrompt(label);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));s.setBackground(surface(Color.WHITE,BORDER));s.setPadding(dp(12),dp(4),dp(12),dp(4));page.addView(s,new LinearLayout.LayoutParams(-1,dp(56)));return s;}
  void fatal(String msg){layout("오류",this::home);text(msg,16,false);}
  void home(){generation++;busy=false;layout("청소년상담사 3급",null);text("기출을 내 속도로",17,false);choice("기출문제 풀기","연도와 과목을 골라 25문항씩",()->examPicker(false));choice("모의고사","선택과목을 포함한 한 회차 150문항",()->examPicker(true));text("기출은 인터넷 연결 없이 풀 수 있습니다.",14,false);}
  JSONObject exam(int year,int session){for(int i=0;i<exams.length();i++){JSONObject e=exams.optJSONObject(i);if(e.optInt("year")==year&&e.optInt("session")==session)return e;}throw new IllegalArgumentException("해당 시험 데이터가 없습니다.");}
  JSONArray subject(JSONObject e,int pos){JSONArray a=e.optJSONArray("questions"),out=new JSONArray();for(int i=pos*25;i<(pos+1)*25;i++)out.put(a.optJSONObject(i));return out;}
  void examPicker(boolean mock){layout(mock?"기출 모의고사":"기출 연도",this::home);Set<Integer> years=new LinkedHashSet<>();for(int i=0;i<exams.length();i++)years.add(exams.optJSONObject(i).optInt("year"));for(int y:years)button(y+"년",()->{if(mock)mockSetup(y);else subjectPicker(y);});}
  void subjectPicker(int year){layout(year+"년 기출",()->examPicker(false));for(int s=0;s<7;s++){final int n=s;if(s==0)text("1교시",19,true);if(s==4)text("2교시",19,true);button(SUBJECTS[s],()->{start(year+"년 · "+SUBJECTS[n],subject(exam(year,n<4?1:2),n<4?n:n-4));});}}
  void mockSetup(int year){layout(year+"년 모의고사",()->examPicker(true));text("필수 5과목 + 선택 1과목 · 150문항",16,false);Spinner s=spinner("선택과목",new String[]{SUBJECTS[5],SUBJECTS[6]});button("모의고사 시작",()->{JSONArray q=new JSONArray();JSONArray first=exam(year,1).optJSONArray("questions");for(int i=0;i<first.length();i++)q.put(first.optJSONObject(i));for(int n:new int[]{0,s.getSelectedItemPosition()+1}){JSONArray part=subject(exam(year,2),n);for(int i=0;i<25;i++)q.put(part.optJSONObject(i));}start(year+"년 모의고사",q);});}
  void start(String t,JSONArray q){title=t;questions=q;answers=new int[q.length()];Arrays.fill(answers,-1);index=0;result=false;quiz();}
  void quiz(){
    layout(title,this::leaveQuiz);screen=2;result=false;
    ((TextView)page.getChildAt(0)).setTextSize(18);
    JSONObject q=questions.optJSONObject(index);text((index+1)+" / "+questions.length()+"문항",14,true);
    ProgressBar position=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);position.setMax(questions.length());position.setProgress(index+1);page.addView(position,new LinearLayout.LayoutParams(-1,dp(8)));
    TextView question=text(q.optString("question"),20,true);question.setPadding(dp(16),dp(20),dp(16),dp(20));question.setBackground(surface(Color.WHITE,BORDER));LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(-1,-2);qp.topMargin=dp(20);qp.bottomMargin=dp(16);question.setLayoutParams(qp);
    questionImage(q);
    RadioGroup group=new RadioGroup(this);page.addView(group);JSONArray options=q.optJSONArray("options");
    for(int i=0;i<options.length();i++){RadioButton b=new RadioButton(this);b.setId(i+100);b.setText((i+1)+". "+options.optString(i));b.setTextColor(Color.BLACK);b.setTextSize(17);b.setButtonDrawable((android.graphics.drawable.Drawable)null);b.setPadding(dp(16),dp(16),dp(16),dp(16));b.setMinHeight(dp(56));b.setLineSpacing(dp(3),1.1f);b.setBackground(interactive());RadioGroup.LayoutParams p=new RadioGroup.LayoutParams(-1,-2);p.bottomMargin=dp(10);group.addView(b,p);}
    if(answers[index]>=0)group.check(answers[index]+100);group.setOnCheckedChangeListener((g,id)->{answers[index]=id-100;answeredText.setText(Arrays.stream(answers).filter(a->a>=0).count()+" / "+questions.length()+"문항 선택");});
    LinearLayout content=page,footer=new LinearLayout(this);footer.setOrientation(LinearLayout.VERTICAL);footer.setPadding(dp(16),dp(8),dp(16),dp(12));footer.setBackground(surface(Color.WHITE,BORDER));shell.addView(footer,new LinearLayout.LayoutParams(-1,-2));page=footer;
    answeredText=text(Arrays.stream(answers).filter(a->a>=0).count()+" / "+questions.length()+"문항 선택",13,true);LinearLayout row=new LinearLayout(this);footer.addView(row);page=content;
    String[] names={"이전","문항표","다음","채점"};for(int i=0;i<4;i++){final int action=i;Button b=control(names[i],()->{if(action==0){index--;quiz();}else if(action==1)omr();else if(action==2){index++;quiz();}else confirmSubmit();});b.setTextSize(14);b.setPadding(dp(2),dp(12),dp(2),dp(12));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(i>0)p.leftMargin=dp(6);row.addView(b,p);b.setEnabled(i!=0||index>0);if(i==2)b.setEnabled(index<questions.length()-1);}
  }
  void leaveQuiz(){new AlertDialog.Builder(this).setMessage("풀이를 종료하고 처음으로 돌아갈까요?").setNegativeButton("계속 풀기",null).setPositiveButton("종료",(d,w)->home()).show();}
  void questionImage(JSONObject q){
    String asset=q.optString("imageAsset"); if(asset.isEmpty())return;
    try(var in=getAssets().open(asset)){
      android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeStream(in);
      if(bitmap==null)throw new java.io.IOException();
      ImageView view=new ImageView(this);view.setImageBitmap(bitmap);view.setAdjustViewBounds(true);view.setContentDescription("문항 자료 · 눌러서 확대");page.addView(view,new LinearLayout.LayoutParams(-1,-2));
      view.setOnClickListener(v->{HorizontalScrollView horizontal=new HorizontalScrollView(this);ScrollView vertical=new ScrollView(this);ImageView full=new ImageView(this);full.setImageBitmap(bitmap);vertical.addView(full,new ScrollView.LayoutParams(Math.max(getResources().getDisplayMetrics().widthPixels,bitmap.getWidth()),-2));horizontal.addView(vertical);new AlertDialog.Builder(this).setView(horizontal).setPositiveButton("닫기",null).show();});
    }catch(Exception e){text("이미지를 읽지 못했습니다.",16,false);}
  }
  void omr(){ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(16),dp(8),dp(16),dp(8));scroll.addView(list);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("문항표 · 색이 있는 칸은 응답 완료").setView(scroll).setNegativeButton("닫기",null).create();LinearLayout row=null;for(int i=0;i<questions.length();i++){if(i%5==0){row=new LinearLayout(this);list.addView(row);}final int n=i;Button b=control(String.valueOf(i+1),()->{dialog.dismiss();index=n;quiz();});b.setPadding(0,dp(8),0,dp(8));b.setSelected(answers[i]>=0);b.setContentDescription((i+1)+"번 · "+(answers[i]>=0?"응답 완료":"미응답")+(i==index?" · 현재 문항":""));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(52),1);p.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(b,p);}dialog.show();}
  boolean correct(JSONObject q,int answer){JSONArray a=q.optJSONArray("answers");if(a!=null){for(int i=0;i<a.length();i++)if(a.optInt(i)==answer)return true;return false;}return answer==q.optInt("answer",-2);}
  void confirmSubmit(){long missing=Arrays.stream(answers).filter(a->a<0).count();new AlertDialog.Builder(this).setMessage(missing>0?"미응답 "+missing+"문항이 있습니다. 채점할까요?":"채점할까요?").setNegativeButton("취소",null).setPositiveButton("채점",(d,w)->results()).show();}
  String answerText(JSONObject q,int a){return a<0?"미응답":(a+1)+". "+q.optJSONArray("options").optString(a);}
  void results(){
    layout("채점 결과",this::home);screen=3;result=true;int count=0;for(int i=0;i<questions.length();i++)if(correct(questions.optJSONObject(i),answers[i]))count++;
    LinearLayout summaryParent=card();text(title,16,true);text(Math.round(count*100f/questions.length())+"점",40,true);text(count+" / "+questions.length()+"문항 정답",16,false);page=summaryParent;
    for(int i=0;i<questions.length();i++){JSONObject q=questions.optJSONObject(i);LinearLayout parent=card();text((i+1)+"번 · "+(correct(q,answers[i])?"정답":"오답"),18,true);text("질문",16,true);text(q.optString("question"),17,false);questionImage(q);String exp=q.optString("explanation");int ref=exp.lastIndexOf("[원문:");if(ref>=0){text(exp.substring(ref),14,false);exp=exp.substring(0,ref).trim();}text("답변 보기",16,true);JSONArray opts=q.optJSONArray("options");for(int j=0;j<opts.length();j++){
      boolean isCorrect=correct(q,j);
      TextView option=text((isCorrect?"✓ 정답\n":"")+answerText(q,j),16,isCorrect);
      option.setPadding(dp(14),dp(12),dp(14),dp(12));
      LinearLayout.LayoutParams optionParams=new LinearLayout.LayoutParams(-1,-2);optionParams.bottomMargin=dp(8);option.setLayoutParams(optionParams);
      if(isCorrect){android.graphics.drawable.GradientDrawable highlight=surface(0xffdcfce7,0xff15803d);highlight.setStroke(dp(2),0xff15803d);option.setBackground(highlight);}
    }text("내 선택",16,true);text(answerText(q,answers[i]),17,false);if(!exp.trim().isEmpty()){final String explanation=exp;button("해설 보기",()->new AlertDialog.Builder(this).setTitle("해설").setMessage(explanation).setPositiveButton("닫기",null).show());}page=parent;}
  }
  static String readText(java.io.InputStream in) throws Exception { try(in; java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()){ byte[] buffer=new byte[8192]; int n; while((n=in.read(buffer))!=-1)out.write(buffer,0,n); return out.toString("UTF-8"); } }
  void background(Job job,Runnable retry){int expected=++generation;busy=true;worker.execute(()->{try{job.run();}catch(Exception e){runOnUiThread(()->{if(isDestroyed()||expected!=generation)return;busy=false;layout("연결에 실패했습니다",this::home);text(e.getMessage()==null?"인터넷 연결을 확인해주세요.":e.getMessage(),16,false);button("다시 시도",retry);});}});}
  int generation=0;
  @Override public void onBackPressed(){if(busy){Toast.makeText(this,"처리 중입니다. 잠시 기다려주세요.",Toast.LENGTH_SHORT).show();return;}if(backAction!=null)backAction.run();else super.onBackPressed();}
}
