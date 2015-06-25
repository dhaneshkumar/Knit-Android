package tutorial;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Typeface;
import android.view.View;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import trumplab.textslate.R;

/**
 * Created by ashish on 25/6/15.
 */
public class ShowcaseCreator {

    static ShowcaseView.Builder getDefaultBuilder(final Activity activity){
        //assume activity not null
        Typeface showcaseFont = Typeface.createFromAsset(activity.getAssets(), "fonts/RobotoCondensed-Bold.ttf");
        ShowcaseView.Builder builder = new ShowcaseView.Builder(activity)
                .setStyle(R.style.ShowcaseView)
                .setFont(showcaseFont)
                .hideButton()
                .hideOnTouchOutside(); //now even the showcase area is outside

        return builder;
    }

    //using ViewTarget
    public static void teacherHighlightCreate(final Activity activity, final View targetView, final View targetView2){

        ShowcaseView.Builder builder = getDefaultBuilder(activity);
        builder.setScaleMultipler(0.25f)
                .setContentTitle("To create a class, click on the highlighted button")
                .setShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {
                        teacherHighlightJoin(activity, targetView2);
                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                    }
                });

        builder.setTarget(new ViewTarget(targetView));

        /*RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        builder.setButtonPosition(layoutParams);*/

        builder.build();
    }

    //using PointTarget
    public static void teacherHighlightJoin(Activity activity, final View targetView){
        ShowcaseView.Builder builder = getDefaultBuilder(activity);
        builder.setScaleMultipler(0.25f)
                .setContentTitle("To join a class, click on the circled button");

        ViewTarget jIconViewTarget = new ViewTarget(targetView);
        Point jIconCenter = jIconViewTarget.getPoint(); //join class action item
        builder.setTarget(new PointTarget(jIconCenter));
        builder.build();
    }


    //Parent signup
    //using ViewTarget
    public static void parentHighlightJoin(final Activity activity, final View targetView, final View targetView2){

        ShowcaseView.Builder builder = getDefaultBuilder(activity);
        builder.setScaleMultipler(0.25f)
                .setContentTitle("To create a class, click on the highlighted button")
                .setShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {
                        parentHighlightJoinedClasses(activity, targetView2);
                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                    }
                });

        builder.setTarget(new ViewTarget(targetView));

        /*RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        builder.setButtonPosition(layoutParams);*/

        builder.build();
    }

    //using PointTarget
    public static void parentHighlightJoinedClasses(Activity activity, final View targetView){
        ShowcaseView.Builder builder = getDefaultBuilder(activity);
        builder.setScaleMultipler(0.25f)
                .setContentTitle("Click here to see your joined classes");

        builder.setTarget(new ViewTarget(targetView));
        builder.build();
    }

    //highlight target along with points on left and right(distance = target view width)
    public static void exampleMultiPointHighlight(Activity activity, final View targetView){
        ShowcaseView.Builder builder = getDefaultBuilder(activity);
        builder.setScaleMultipler(0.25f)
                .setContentTitle("To join a class, click on the circled button");

        ViewTarget jIconViewTarget = new ViewTarget(targetView);
        Point jIconCenter = jIconViewTarget.getPoint(); //join class action item
        Point auxCenter1 = new Point(jIconCenter.x + targetView.getWidth(), jIconCenter.y);
        Point auxCenter2 = new Point(jIconCenter.x - targetView.getWidth(), jIconCenter.y);
        builder.setTarget(new PointTarget(jIconCenter));

        builder.setAuxPoints(new Point[]{auxCenter1, auxCenter2}); //extra points to highlight

        builder.build();
    }
}
