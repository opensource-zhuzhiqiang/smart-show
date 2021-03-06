package com.coder.zzq.smartshow.bar.core;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.RestrictTo;

import com.coder.zzq.toolkit.Utils;
import com.coder.zzq.toolkit.log.EasyLogger;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class BarDelegate<Bar, ViewParam, BarSetting extends BarSettingImpl> implements IBarShow, View.OnClickListener, Runnable {

    protected Context mPageContext;
    protected CharSequence mCurMsg;
    protected CharSequence mCurActionText;
    protected View.OnClickListener mOnActionClickListener;
    @DrawableRes
    protected int mCurrentIcon;
    @DrawableRes
    protected int mTempIcon;
    protected Rect mIconRange;
    protected SparseArray<Drawable> mCacheDrawables;
    protected int mDuration;
    protected View mBaseTraceView;
    protected Bar mBar;

    protected BarDelegate() {
        mCurMsg = "";
        mCurActionText = "";
    }

    protected IBarShow getFromView(View view) {
        if (mBar == null || mBaseTraceView != view || isDismissByGesture()) {
            mBaseTraceView = view;
            rebuildBar(view);
        }
        return this;
    }

    protected abstract boolean isDismissByGesture();


    private void rebuildBar(View view) {

        if (view == null) {
            return;
        }

        mBar = createBar(view);
        if (mPageContext instanceof IBarShowCallback) {
            setShowCallback();
        }


        if (hasBarSetting()) {
            TextView msgView = getMsgView();
            msgView.setTextColor(getBarSetting().getMsgColor());
            msgView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getBarSetting().getMsgTextSizeSp());
            Button actionView = getActionView();
            actionView.setTextColor(getBarSetting().getActionColor());
            actionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getBarSetting().getActionSizeSp());
            if (getBarSetting().getProcessBarCallback() != null) {
                getBarSetting().getProcessBarCallback().processBarView((ViewGroup) getBarView(), msgView, actionView);
            }
            setup();
        }
        EasyLogger.d("rebuild the bar" + Utils.getObjectDesc(mBar));
    }


    public abstract void setup();

    protected abstract ViewParam getBarView();

    protected abstract Button getActionView();

    protected abstract TextView getMsgView();


    protected abstract Bar createBar(View view);

    protected abstract void setShowCallback();


    public void destroy(Activity activity) {
        if (mPageContext == activity) {
            EasyLogger.d("the bar" + Utils.getObjectDesc(mBar) + "has recycled");
            mCurMsg = "";
            mCurActionText = "";
            mOnActionClickListener = null;
            mBar = null;
            mPageContext = null;
            mBaseTraceView = null;
            if (mCacheDrawables != null) {
                mCacheDrawables.clear();
                mCacheDrawables = null;
            }
            mIconRange = null;

        }
    }


    private void showHelper(CharSequence msg, CharSequence actionText, View.OnClickListener onActionClickListener, int duration) {

        if (mBaseTraceView == null) {
            return;
        }

        msg = msg == null ? "" : msg;
        actionText = actionText == null ? "" : actionText;
        onActionClickListener = onActionClickListener == null ? this : onActionClickListener;

        boolean appearanceChanged = appearanceChanged(msg, actionText, mTempIcon);

        setting(msg, actionText, onActionClickListener, mTempIcon, duration);

        if ((isShowing() && appearanceChanged) || mBaseTraceView.getVisibility() != View.VISIBLE) {
            //如果Snackbar正在显示，且内容发生了变化，先隐藏掉再显示，具有切换效果
            dismissAndShowAgain();
        } else {
            //如果Snackbar没有显示或者“样貌”没有发生改变，正常显示即可
            normalShow();
        }

    }

    protected void dismissAndShowAgain() {
        dismiss();
        mBaseTraceView.postDelayed(this, 400);
        EasyLogger.d("dismiss and show again");
    }

    @Override
    public void run() {
        normalShow();
    }


    private boolean appearanceChanged(CharSequence msg, CharSequence actionText, @DrawableRes int icon) {
        return !mCurMsg.equals(msg) || !mCurActionText.equals(actionText) || mCurrentIcon != icon;
    }

    //正常显示bar
    protected abstract void normalShow();

    protected SparseArray<Drawable> getCacheDrawables() {
        if (mCacheDrawables == null) {
            mCacheDrawables = new SparseArray<>();
        }
        return mCacheDrawables;
    }

    private void setting(CharSequence msg, CharSequence actionText, View.OnClickListener onActionClickListener, @DrawableRes int icon, int duration) {
        mCurMsg = msg;
        mCurActionText = actionText;
        mOnActionClickListener = onActionClickListener;
        mCurrentIcon = icon;
        if (icon != 0 && getCacheDrawables().get(icon, null) == null) {
            getCacheDrawables().put(icon, Utils.getDrawableFromRes(icon));
        }
        mDuration = duration;
        mTempIcon = 0;
    }

    public Rect getIconRange() {
        if (mIconRange == null) {
            mIconRange = new Rect();
        }
        return mIconRange;
    }

    protected void setMsgIcon() {
        Drawable drawable = mCurrentIcon == 0 ? null : getCacheDrawables().get(mCurrentIcon);
        if (drawable != null) {
            getMsgView().getPaint().getTextBounds(mCurMsg.toString(), 0, mCurMsg.length(), getIconRange());
            int drawableSize = getIconRange().height() + Utils.dpToPx(2);
            drawable.setBounds(0, 0, drawableSize, drawableSize);
        }
        getMsgView().setCompoundDrawablePadding(Utils.dpToPx(8));
        getMsgView().setCompoundDrawables(drawable, null, null, null);
    }

    @Override
    public void show(CharSequence msg) {
        show(msg, null, null);
    }


    @Override
    public void show(CharSequence msg, CharSequence actionText) {
        show(msg, actionText, null);
    }


    @Override
    public void show(CharSequence msg, CharSequence actionText, View.OnClickListener onActionClickListener) {
        showHelper(msg, actionText, onActionClickListener, getShortDuration());
    }


    @Override
    public void showLong(CharSequence msg) {
        showLong(msg, null);
    }


    @Override
    public void showLong(CharSequence msg, CharSequence actionText) {
        showLong(msg, actionText, null);
    }

    @Override
    public void showLong(CharSequence msg, CharSequence actionText, View.OnClickListener onActionClickListener) {
        showHelper(msg, actionText, onActionClickListener, getLongDuration());
    }


    @Override
    public void showIndefinite(CharSequence msg) {
        showIndefinite(msg, hasBarSetting() ? getBarSetting().getDefaultActionTextForIndefinite() : "确定");
    }


    @Override
    public void showIndefinite(CharSequence msg, CharSequence actionText) {
        showIndefinite(msg, actionText, null);
    }


    @Override
    public void showIndefinite(CharSequence msg, CharSequence actionText, View.OnClickListener onActionClickListener) {
        showHelper(msg, actionText, onActionClickListener, getIndefiniteDuration());
    }

    @Override
    public void show(int msg) {
        show(Utils.getStringFromRes(msg));
    }

    @Override
    public void show(int msg, int actionText) {
        show(Utils.getStringFromRes(msg), Utils.getStringFromRes(actionText));
    }

    @Override
    public void show(int msg, int actionText, View.OnClickListener onActionClickListener) {
        show(Utils.getStringFromRes(msg), Utils.getStringFromRes(actionText), onActionClickListener);
    }

    @Override
    public void showLong(int msg) {
        showLong(Utils.getStringFromRes(msg));
    }

    @Override
    public void showLong(int msg, int actionText) {
        showLong(Utils.getStringFromRes(msg), Utils.getStringFromRes(actionText));
    }

    @Override
    public void showLong(int msg, int actionText, View.OnClickListener onActionClickListener) {
        showLong(Utils.getStringFromRes(msg), Utils.getStringFromRes(actionText), onActionClickListener);
    }

    @Override
    public void showIndefinite(int msg) {
        showIndefinite(Utils.getStringFromRes(msg));
    }

    @Override
    public void showIndefinite(int msg, int actionText) {
        showIndefinite(Utils.getStringFromRes(msg), Utils.getStringFromRes(actionText));
    }

    @Override
    public void showIndefinite(int msg, int actionText, View.OnClickListener onActionClickListener) {
        showIndefinite(Utils.getStringFromRes(msg), Utils.getStringFromRes(actionText), onActionClickListener);
    }

    @Override
    public IBarShow icon(int icon) {
        mTempIcon = icon;
        return this;
    }

    @Override
    public void onClick(View v) {

    }

    protected abstract int getShortDuration();

    protected abstract int getLongDuration();

    protected abstract int getIndefiniteDuration();


    public abstract boolean isShowing();

    public abstract void dismiss();


    public boolean isDismissOnLeave() {
        return hasBarSetting() && getBarSetting().isDismissOnLeave();
    }


    public abstract boolean hasBarSetting();


    protected abstract BarSetting createBarSetting();

    public abstract BarSetting getBarSetting();

    public void onLeave(Activity activity) {
        if (mPageContext == activity) {
            dismiss();
        }
    }
}
