package com.zeus97x.zpet;

import android.animation.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import java.io.InputStream;

/** Low-cost illustration motion prototype, not frame-by-frame animation. */
public final class CreatureView extends ImageView {
    private ValueAnimator idle;
    private AnimatorSet effect;
    private boolean reducedMotion;
    public CreatureView(Context context,String monster,boolean reduced) {
        super(context); reducedMotion=reduced;
        setScaleType(ScaleType.FIT_CENTER); setContentDescription(monster+" · "+MonsterCatalog.stage(monster)+" · "+BranchPalette.label(monster));
        float[] palette=BranchPalette.matrix(monster);
        if(palette!=null) setColorFilter(new android.graphics.ColorMatrixColorFilter(palette));
        try {
            BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
            try(InputStream in=context.getAssets().open(MonsterCatalog.asset(monster))) { BitmapFactory.decodeStream(in,null,options); }
            options.inSampleSize=1;
            while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>768) options.inSampleSize*=2;
            options.inJustDecodeBounds=false;
            try(InputStream in=context.getAssets().open(MonsterCatalog.asset(monster))) {
                Bitmap bitmap=BitmapFactory.decodeStream(in,null,options); setImageBitmap(bitmap);
            }
        } catch(java.io.IOException error) { setContentDescription(monster+" · artwork unavailable"); }
    }
    private boolean motionAllowed() { return !reducedMotion && ValueAnimator.areAnimatorsEnabled(); }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); startIdle(); }
    private void startIdle() {
        if(!motionAllowed() || idle!=null || getWindowVisibility()!=VISIBLE) return;
        idle=ValueAnimator.ofFloat(0,1); idle.setDuration(2200); idle.setRepeatCount(ValueAnimator.INFINITE);
        idle.setRepeatMode(ValueAnimator.REVERSE); idle.setInterpolator(new AccelerateDecelerateInterpolator());
        idle.addUpdateListener(a -> {
            float t=(float)a.getAnimatedValue(); setScaleX(1+0.015f*t); setScaleY(1+0.015f*t);
            setTranslationY(-4*getResources().getDisplayMetrics().density*t);
        }); idle.start();
    }
    private void stopIdle() { if(idle!=null) { idle.cancel(); idle=null; } setTranslationY(0); setScaleX(1); setScaleY(1); }
    public void preview(String kind) {
        if(!motionAllowed()) return;
        if(effect!=null) { effect.removeAllListeners(); effect.cancel(); }
        stopIdle(); setTranslationX(0); setAlpha(1);
        effect=new AnimatorSet();
        if(kind.equals("Hit")) effect.playTogether(ObjectAnimator.ofFloat(this,"translationX",0,-10,10,-6,0));
        else if(kind.equals("Attack")) effect.playTogether(ObjectAnimator.ofFloat(this,"translationX",0,24,0),ObjectAnimator.ofFloat(this,"scaleX",1,1.05f,1));
        else effect.playTogether(ObjectAnimator.ofFloat(this,"alpha",1,0.2f,1),ObjectAnimator.ofFloat(this,"scaleX",1,0.9f,1.08f,1),ObjectAnimator.ofFloat(this,"scaleY",1,0.9f,1.08f,1));
        effect.setDuration(650);
        effect.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) { setTranslationX(0); setAlpha(1); setScaleX(1); setScaleY(1); if(isAttachedToWindow()) startIdle(); }
        }); effect.start();
    }
    @Override protected void onDetachedFromWindow() {
        if(effect!=null) { effect.removeAllListeners(); effect.cancel(); effect=null; }
        stopIdle(); super.onDetachedFromWindow();
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if(visibility==VISIBLE && isAttachedToWindow()) startIdle();
        else {
            if(effect!=null) { effect.removeAllListeners(); effect.cancel(); effect=null; }
            stopIdle(); setTranslationX(0); setAlpha(1);
        }
    }
}
