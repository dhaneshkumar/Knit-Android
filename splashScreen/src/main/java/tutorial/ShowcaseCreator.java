package tutorial;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import joinclasses.JoinClassDialog;
import trumplab.textslate.R;
import trumplabs.schoolapp.CreateClassDialog;

/**
 * Created by ashish on 25/6/15.
 */
public class ShowcaseCreator {

    static ShowcaseView.Builder getDefaultBuilder(final Activity activity){
        //assume activity not null
        Typeface showcaseFont = Typeface.createFromAsset(activity.getAssets(), "fonts/RobotoCondensed-Bold.ttf");
        ShowcaseView.Builder builder = new ShowcaseView.Builder(activity)
                .setStyle(R.style.ShowcaseView)
                .setFont(showcaseFont);
                //.hideButton()
                //.hideOnTouchOutside(); //now even the showcase area is outside

        //default next button(image) position at right edge center
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        builder.setButtonPosition(layoutParams);

        return builder;
    }

    //using ViewTarget
    public static void teacherHighlightCreate(final Activity activity, final View targetView, final View targetView2){
        //post on target i.e when target is ready and hence its position and size are set

        targetView.post(new Runnable() {
            @Override
            public void run() {
                ShowcaseView.Builder builder = getDefaultBuilder(activity);
                builder.setScaleMultipler(0.25f)
                        .setContentText("To create a class, click on the highlighted button")
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

                //builder.setTarget(jIconViewTarget); //last
//                Log.d("_SHOWCASE_", "jIconCenter x=" + jIconCenter.x + ",y=" + jIconCenter.y);
                ViewTarget target = new ViewTarget(targetView);
                Point center = target.getPoint(); //join class action item
                builder.setTarget(new PointTarget(center));

                double scalingFactor = 2.0 / 3; //according to pointer width and action bar icon width
                final ShowcaseView showcaseView = builder.getShowcaseView();
                showcaseView.setPointer(center.x - (int) (targetView.getWidth() * scalingFactor), center.y + targetView.getHeight());
                showcaseView.setDescription("To create a class, click on the highlighted button");

                builder.build();
            }
        });
    }

    //using PointTarget
    public static void teacherHighlightJoin(final Activity activity, final View targetView){
        //post on target i.e when target is ready and hence its position and size are set
        targetView.post(new Runnable() {
            @Override
            public void run() {
                ShowcaseView.Builder builder = getDefaultBuilder(activity);
                builder.setScaleMultipler(0.25f)
                        .setContentText("To join a class, click on the circled button");

                builder.setScaleMultipler(0.25f)
                        .setContentText("To create a class, click on the highlighted button")
                        .setShowcaseEventListener(new OnShowcaseEventListener() {
                            @Override
                            public void onShowcaseViewHide(ShowcaseView showcaseView) {
                                //show create class dialog
                                if (activity != null) {
                                    FragmentManager fm = ((FragmentActivity) (activity)).getSupportFragmentManager(); //MyActionBarActivity (our base class) is FragmentActivity derivative
                                    CreateClassDialog createClassDialog = new CreateClassDialog();
                                    Bundle args = new Bundle();
                                    args.putString("flag", "SIGNUP");
                                    createClassDialog.setArguments(args);
                                    createClassDialog.show(fm, "create Class");
                                }
                            }

                            @Override
                            public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                            }

                            @Override
                            public void onShowcaseViewShow(ShowcaseView showcaseView) {

                            }
                        });

                ViewTarget target = new ViewTarget(targetView);
                Point center = target.getPoint(); //join class action item
                //Point center = new Point(center2.x + targetView.getWidth(), center2.y); //reach overflow icon
                //Log.d("_SHOWCASE_", "jIconCenter x=" + centerWithOffset.x + ", y=" + centerWithOffset.y + ", w=" + targetView.getWidth());
                builder.setTarget(new PointTarget(center));

                ShowcaseView showcaseView1 = builder.getShowcaseView();

                double scalingFactor = 2.0/3;
                showcaseView1.setPointer(center.x - (int) (targetView.getWidth() * scalingFactor) , center.y + targetView.getHeight());
                showcaseView1.setDescription("Click here to join a class");

                builder.build();
            }
        });
    }


    //Parent signup
    //using ViewTarget
    public static void parentHighlightJoin(final Activity activity, final View targetView, final View targetView2){

        targetView.post(new Runnable() {
            @Override
            public void run() {
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

                ViewTarget target = new ViewTarget(targetView);
                Point center = target.getPoint(); //join class action item
                builder.setTarget(new PointTarget(center));

                double scalingFactor = 2.0/3; //according to pointer width and action bar icon width
                final ShowcaseView showcaseView = builder.getShowcaseView();
                showcaseView.setPointer(center.x - (int) (targetView.getWidth() * scalingFactor), center.y + targetView.getHeight());
                showcaseView.setDescription("Click here to join a new class");

                builder.build();
            }
        });
    }

    //using PointTarget
    public static void parentHighlightJoinedClasses(final Activity activity, final View targetView){
        targetView.post(new Runnable() {
            @Override
            public void run() {
                ShowcaseView.Builder builder = getDefaultBuilder(activity);
                builder.setScaleMultipler(0.25f)
                        .setContentTitle("Click here to see your joined classes")
                        .setShowcaseEventListener(new OnShowcaseEventListener() {
                            @Override
                            public void onShowcaseViewHide(ShowcaseView showcaseView) {
                                //show join class dialog
                                if(activity != null){
                                    FragmentManager fm = ((FragmentActivity) (activity)).getSupportFragmentManager(); //MyActionBarActivity (our base class) is FragmentActivity derivative
                                    JoinClassDialog joinClassDialog = new JoinClassDialog();
                                    joinClassDialog.show(fm, "Join Class");
                                }
                            }

                            @Override
                            public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                            }

                            @Override
                            public void onShowcaseViewShow(ShowcaseView showcaseView) {

                            }
                        });
                ViewTarget target = new ViewTarget(targetView);
                Point center = target.getPoint(); //join class action item
                //Log.d("_SHOWCASE_", "jIconCenter x=" + centerWithOffset.x + ", y=" + centerWithOffset.y + ", w=" + targetView.getWidth());
                builder.setTarget(new PointTarget(center));

                ShowcaseView showcaseView1 = builder.getShowcaseView();

                double scalingFactor = 2.0/3;
                showcaseView1.setPointer(center.x - (int) (targetView.getWidth() * scalingFactor), center.y + targetView.getHeight());
                showcaseView1.setDescription("Click here to see your joined classes");

                builder.build();
            }
        });
    }

    //using PointTarget
    public static void parentHighlightResponseButtons(final Activity activity, final View likeView, final View confuseView){
        likeView.post(new Runnable() {
            @Override
            public void run() {
                ShowcaseView.Builder builder = getDefaultBuilder(activity);
                builder.setScaleMultipler(0.30f)
                        .setContentTitle("Click here to see your joined classes");

                ViewTarget target = new ViewTarget(likeView);
                Point center = target.getPoint();

                ViewTarget target2 = new ViewTarget(confuseView);
                Point center2 = target2.getPoint();

                //Log.d("_SHOWCASE_", "jIconCenter x=" + centerWithOffset.x + ", y=" + centerWithOffset.y + ", w=" + targetView.getWidth());
                builder.setTarget(new PointTarget(center2)); //main target
                builder.setAuxPoints(new Point[]{center});

                ShowcaseView showcaseView1 = builder.getShowcaseView();

                double scalingFactor = 2.0/3;
                showcaseView1.setPointer(center2.x - (int) (likeView.getWidth() * scalingFactor), center.y + likeView.getHeight());
                showcaseView1.flipPointerHorizontally();
                showcaseView1.setDescription("Use these two buttons to respond to messages. Use thumbs up to like and '?' to show you are confused");

                builder.build();
            }
        });
    }

    //in outbox(targets are the text containing the counts. So center showcase at the right end of these views
    // so that we are roughly at center of combined view(count textview + icon imageview)
    public static void teacherHighlightResponseButtons(final Activity activity, final View likeView, final View confuseView){
        likeView.post(new Runnable() {
            @Override
            public void run() {
                ShowcaseView.Builder builder = getDefaultBuilder(activity);
                builder.setScaleMultipler(0.25f)
                        .setContentTitle("Click here to see your joined classes");

                ViewTarget target = new ViewTarget(likeView);
                Point center = target.getPoint();
                center = new Point(center.x + likeView.getWidth()/2, center.y);

                ViewTarget target2 = new ViewTarget(confuseView);
                Point center2 = target2.getPoint();
                center2 = new Point(center2.x + likeView.getWidth()/2, center2.y); //width of both like and confused view are same

                //Log.d("_SHOWCASE_", "jIconCenter x=" + centerWithOffset.x + ", y=" + centerWithOffset.y + ", w=" + targetView.getWidth());
                builder.setTarget(new PointTarget(center2)); //main target
                builder.setAuxPoints(new Point[]{center});

                ShowcaseView showcaseView1 = builder.getShowcaseView();

                double scalingFactor = 2.0/3;
                showcaseView1.setPointer(center2.x - (int) (likeView.getWidth() * scalingFactor), center.y + likeView.getHeight());
                showcaseView1.flipPointerHorizontally();
                showcaseView1.setDescription("Here you can see how many parents/students like your post or are confused about it. They can respond using only two buttons");

                builder.build();
            }
        });
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
