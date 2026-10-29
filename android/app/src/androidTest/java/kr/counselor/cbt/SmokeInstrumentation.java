package kr.counselor.cbt;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import org.json.*;

/** Offline smoke test: exercises the actual Activity and bundled official answers. */
public class SmokeInstrumentation extends Instrumentation {
  @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
  void check(boolean ok, String message) { if(!ok) throw new AssertionError(message); }
  @Override public void onStart() {
    Bundle report = new Bundle();
    try {
      MainActivity a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      runOnMainSync(()->{
        check(a.exams.length()==18,"18 exam sessions");
        int total=0;
        for(int i=0;i<a.exams.length();i++) {
          JSONObject e=a.exams.optJSONObject(i); JSONArray qs=e.optJSONArray("questions"); total+=qs.length();
          check(qs.length()==(e.optInt("session")==1?100:75),"session question count");
          for(int j=0;j<qs.length();j++) {
            JSONObject q=qs.optJSONObject(j);
            check(q.optJSONArray("options").length()==5,"five options");
            check(!q.optString("question").trim().isEmpty(),"question text");
            check(a.correct(q,q.optInt("answer")),"official answer normalization");
            check(!a.correct(q,-1),"unanswered cannot be correct");
          }
        }
        check(total==1575,"1575 bundled questions");
        a.start("Test",a.subject(a.exam(2020,1),0));
        check(a.answers.length==25 && a.screen==2,"quiz initialized");
        for(int i=0;i<a.page.getChildCount();i++)if(a.page.getChildAt(i) instanceof RadioGroup)((RadioGroup)a.page.getChildAt(i)).check(102);
        check(a.answers[0]==2,"radio selection persists");
        a.quiz(); check(a.answers[0]==2,"redraw preserves answer");
        a.results(); check(a.screen==3 && a.result,"results render");
        a.mockSetup(2020);
        ((Button)a.page.getChildAt(a.page.getChildCount()-1)).performClick();
        check(a.questions.length()==150,"full mock exam");
        a.home();
        a.finish();
      });
      report.putString("stream","PASS: 1575 official questions, answer selection, results, 150-question mock.\n");
      finish(Activity.RESULT_OK,report);
    } catch(Throwable e) { report.putString("stream","FAIL: "+e); finish(Activity.RESULT_CANCELED,report); }
  }
}
