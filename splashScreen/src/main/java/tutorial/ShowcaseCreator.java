package tutorial;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import additionals.InviteToClassDialog;
import joinclasses.JoinClassDialog;
import trumplab.textslate.R;
import trumplabs.schoolapp.CreateClassDialog;

/**
 * Created by ashish on 25/6/15.
 */
public class ShowcaseCreator {

    public static final String LOGTAG = "_TUTORIAL_";

    static ShowcaseView.Builder getDefaultBuilder(final Activity activity){
        //assume activity not null
        Typeface showcaseFont = Typeface.createFromAsset(activity.getAssets(), "fonts/RobotoCondensed-Bold.ttf");
        ShowcaseView.Builder builder = new ShowcaseView.Builder(activity)
                .setStyle(R.style.ShowcaseView)
                .setFont(showcaseFont);
                //.hideButton()
                //.hideOnTouchOutside(); //now even the showcase area is outside

        //default next button(image) position at right edge center
        /*RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        builder.setButtonPosition(layoutParams);
        */
        return builder;
    }

    //using ViewTarget
    public static void teacherHighlightCreate(final Activity activity, final View targetView, final View targetView2){
        //post on target i.e when target is ready and hence its position and size are set

        ShowcaseView.isVisible = true;

        targetView.postDelayed(new Runnable() {
            @Override
            public void run() {
                ShowcaseView.Builder builder = getDefaultBuilder(activity);
                builder.setScaleMultipler(0.25f)
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
                showcaseView.setDescription("To create a classroom, click here");

                builder.build();
            }
        }, 100);
    }

    //using PointTarget
    public static void teacherHighlightJoin(final Activity activity, final View targetView){
        ShowcaseView.isVisible = true;

        //post on target i.e when target is ready and hence its position and size are set
        targetView.postDelayed(new Runnable() {
            @Override
            public void run() {
                ShowcaseView.Builder builder = getDefaultBuilder(activity);

                builder.setScaleMultipler(0.25f)
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
                showcaseView1.setDescription("Click here to join a classroom");

                builder.build();
            }
        }, 100);
    }


    //Parent signup
    //using ViewTarget
    public static void parentHighlightJoin(final Activity activity, final View targetView, final View targetView2){
        ShowcaseView.isVisible = true;


        targetView.postDelayed(new Runnable() {
            @Override
            public void run() {
                ShowcaseView.Builder builder = getDefaultBuilder(activity);
                builder.setScaleMultipler(0.25f)
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
                showcaseView.setDescription("Click here to join a classroom");

                builder.build();
            }
        }, 100);
    }

    //using PointTarget
    public static void parentHighlightJoinedClasses(final Activity activity, final View targetView){
        ShowcaseView.isVisible = true;

        targetView.postDelayed(new Runnable() {
            @Override
            public void run() {
                ShowcaseView.Builder builder = getDefaultBuilder(activity);
                builder.setScaleMultipler(0.25f)
                        .setShowcaseEventListener(new OnShowcaseEventListener() {
                            @Override
                            public void onShowcaseViewHide(ShowcaseView showcaseView) {
                                //show join class dialog
                                if (activity != null) {
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

                double scalingFactor = 2.0 / 3;
                showcaseView1.setPointer(center.x - (int) (targetView.getWidth() * scalingFactor), center.y + targetView.getHeight());
                showcaseView1.setDescription("Click here to see your joined classrooms");

                builder.build();
            }
        }, 100);
    }

    //Parent options
    //view is the joined_classes(parent) or join class (teacher) - options ... is one block away
    public static void highlightOptions(final Activity activity, final View targetView, final boolean isParent){
        ShowcaseView.isVisible = true;


        targetView.postDelayed(new Runnable() {
            @Override
            public void run() {
                ShowcaseView.Builder builder = getDefaultBuilder(activity);
                builder.setScaleMultipler(0.25f);

                ViewTarget target = new ViewTarget(targetView);
                Point center = target.getPoint(); //joined class action item

                center = new Point(center.x + targetView.getWidth(), center.y); //options menu action item
                builder.setTarget(new PointTarget(center));

                final ShowcaseView showcaseView = builder.getShowcaseView();
                if(isParent) {
                    showcaseView.setPointer(targetView.getWidth() / 4, center.y + targetView.getHeight(), 2);
                    showcaseView.setDescription("Click on the highlighted menu to access other options : edit your profile, give feedback or spread the word");
                }
                else {
                    showcaseView.setPointer(targetView.getWidth() / 4, center.y + targetView.getHeight(), 1);
                    showcaseView.setDescription("Click on the highlighted menu to access other options : edit your profile, give feedback or spread the word");
                }

                builder.build();
            }
        }, 100);
    }
    /*//using PointTarget
    public static void parentHighlightResponseButtons(final Activity activity, final View likeView, final View confuseView){
        likeView.postDelayed(new Runnable() {
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

                boolean above = true; //pointer and text will be above, hence we need to calculate the x,y properly for pointer
                Display mDisplay = activity.getWindowManager().getDefaultDisplay();
                int windowHeight = mDisplay.getHeight(); //usable area

                //This is because for lollypop, getDecorView returns navigation bar also, hence factor it out
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int navBarHeight = Tools.getNavigationBarSize(Application.getAppContext()).y;
                    windowHeight += navBarHeight; //updated window height
                }

                double scalingFactor = 2.0 / 3;
                int pointerXLeft = center2.x - (int) (likeView.getWidth() * scalingFactor);
                int pointerYBelow = windowHeight - center2.y + likeView.getHeight();
                Log.d("_TUTORIAL_", "window h=" + windowHeight + ", center x=" + center2.x + ", y=" + center2.y + " likeview h=" + likeView.getHeight() + ", pointerYBelow=" + pointerYBelow);

                //center.y + likeView.getHeight()
                showcaseView1.setPointer(pointerXLeft, pointerYBelow, above);
                showcaseView1.flipPointer();
                showcaseView1.setDescription("Use these two buttons to respond to messages. Use thumbs up to like and '?' to show you are confused", above);

                builder.build();
            }
        }, 100);
    }
*/
    public static void parentHighlightResponseButtonsNew(final Activity activity, final View likeView, final Bundle bundle){
        ShowcaseView.isVisible = true;

        likeView.post(new Runnable() {
            @Override
            public void run() {
                final LinearLayout buttonRow = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.messages_response_row_tutorial, null);

                ShowcaseView.Builder builder = getDefaultBuilder(activity);
                builder.setTarget(Target.NONE); //no target

                builder.setShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {
                        //show join class dialog
                        if(activity == null) return; //check activity become null after delay
                        FragmentManager fm = ((FragmentActivity) (activity)).getSupportFragmentManager(); //MyActionBarActivity (our base class) is FragmentActivity derivative
                        InviteToClassDialog inviteToClassDialog = new InviteToClassDialog();
                        inviteToClassDialog.setArguments(bundle);
                        inviteToClassDialog.show(fm, "Invite others");
                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                    }
                });

                ShowcaseView showcaseView1 = builder.getShowcaseView();

                //center.y + likeView.getHeight()
                showcaseView1.addButtonRow(buttonRow);
                showcaseView1.setPointerAbove(buttonRow);
                showcaseView1.flipPointer();
                showcaseView1.setDescription("Use these two buttons to respond to messages. Use thumbs-up to like and '?' to show you are confused", true);

                builder.build();
            }
        });
    }

    //in outbox(targets are the text containing the counts. So center showcase at the right end of these views
    // so that we are roughly at center of combined view(count textview + icon imageview)
    /*public static void teacherHighlightResponseButtons(final Activity activity, final View likeView, final View confuseView){
        likeView.postDelayed(new Runnable() {
            @Override
            public void run() {
                ShowcaseView.Builder builder = getDefaultBuilder(activity);
                builder.setScaleMultipler(0.25f)
                        .setContentTitle("Click here to see your joined classes");

                ViewTarget target = new ViewTarget(likeView);
                Point center = target.getPoint();
                center = new Point(center.x + likeView.getWidth() / 2, center.y);

                ViewTarget target2 = new ViewTarget(confuseView);
                Point center2 = target2.getPoint();
                center2 = new Point(center2.x + likeView.getWidth() / 2, center2.y); //width of both like and confused view are same

                //Log.d("_SHOWCASE_", "jIconCenter x=" + centerWithOffset.x + ", y=" + centerWithOffset.y + ", w=" + targetView.getWidth());
                builder.setTarget(new PointTarget(center2)); //main target
                builder.setAuxPoints(new Point[]{center});

                ShowcaseView showcaseView1 = builder.getShowcaseView();

                boolean above = true; //pointer and text will be above, hence we need to calculate the x,y properly for pointer
                Display mDisplay = activity.getWindowManager().getDefaultDisplay();
                int windowHeight = mDisplay.getHeight();


                //This is because for lollypop, getDecorView returns navigation bar also, hence factor it out
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int navBarHeight = Tools.getNavigationBarSize(Application.getAppContext()).y;
                    windowHeight += navBarHeight; //updated window height
                }

                double scalingFactor = 2.0 / 3;
                int pointerXLeft = center2.x - (int) (likeView.getWidth() * scalingFactor);
                int pointerYBelow = windowHeight - center2.y + likeView.getHeight();
                Log.d("_TUTORIAL_", "window h=" + windowHeight + ", center x=" + center2.x + ", y=" + center2.y + " likeview h=" + likeView.getHeight() + ", pointerYBelow=" + pointerYBelow);

                //center.y + likeView.getHeight()
                showcaseView1.setPointer(pointerXLeft, pointerYBelow, above);
                showcaseView1.flipPointer();
                showcaseView1.setDescription("Here you can see how many parents/students like your post or are confused about it. They can respond using only two buttons", above);

                builder.build();
            }
        }, 100);
    }
*/
    public static void teacherHighlightResponseButtonsNew(final Activity activity, final View likeView){
        ShowcaseView.isVisible = true;

        likeView.post(new Runnable() {
            @Override
            public void run() {
                final LinearLayout buttonRow = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.outbox_response_row_tutorial, null);

                ShowcaseView.Builder builder = getDefaultBuilder(activity);
                builder.setTarget(Target.NONE); //no target
                ShowcaseView showcaseView1 = builder.getShowcaseView();

                //center.y + likeView.getHeight()
                showcaseView1.addButtonRow(buttonRow);
                showcaseView1.setPointerAbove(buttonRow);
                showcaseView1.flipPointer();
                showcaseView1.setDescription("For each post, you can see how many parents/students like it or are confused about it. They can respond using only two buttons - like or confuse", true);

                builder.build();
            }
        });
    }

    public static void teacherComposeTutorial(final Activity activity){
        ShowcaseView.isVisible = true;
        ShowcaseView.Builder builder = getDefaultBuilder(activity);
        builder.setTarget(Target.NONE); //no target
        ShowcaseView showcaseView1 = builder.getShowcaseView();

        showcaseView1.fixButton(); //need to call since not calling setPointer
        showcaseView1.showOnlyDescription("You can use Knit to send bulk sms to parents/students who don't have smartphones if they subscribe via sms. \n \n And don't worry if you send a message when you are offline, we will send it automatically whenever you come online");

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
