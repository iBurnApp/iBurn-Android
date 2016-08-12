package com.gaiagps.iburn;

import android.annotation.TargetApi;
import android.app.Notification;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

import org.prx.playerhater.plugins.TouchableNotificationPlugin;

/**
 * Created by dbro on 8/12/16.
 */
@TargetApi(15)
public class MaterialExpandableNotificationPlugin extends TouchableNotificationPlugin {

    private RemoteViews mExpandedView;

    protected Notification getNotification() {
        if(this.mNotification == null) {
            this.mNotification = super.getNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                this.mNotification.bigContentView = this.getExpandedView();
            }
        }

        return this.mNotification;
    }

    protected Notification.Builder getNotificationBuilder() {
        Notification.Builder builder = new Notification.Builder(this.getContext()).setAutoCancel(false).setSmallIcon(org.prx.playerhater.R.drawable.zzz_ph_ic_notification).setTicker("Playing: " + this.mNotificationTitle).setContentIntent(this.mContentIntent).setOngoing(true).setWhen(0L).setOnlyAlertOnce(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setCustomContentView(this.getNotificationView());
            builder.setCustomBigContentView(getExpandedView());
        } else {
            builder.setContent(this.getNotificationView());
        }
        return builder;
    }

    private RemoteViews getExpandedView() {
        if(this.mExpandedView == null) {
            this.mExpandedView = new RemoteViews(this.getContext().getPackageName(), org.prx.playerhater.R.layout.zzz_ph_jbb_notification);
            this.setListeners(this.mExpandedView);
            this.mExpandedView.setTextViewText(org.prx.playerhater.R.id.zzz_ph_notification_title, this.mNotificationTitle);
            this.mExpandedView.setTextViewText(org.prx.playerhater.R.id.zzz_ph_notification_text, this.mNotificationText);
            this.mExpandedView.setImageViewUri(org.prx.playerhater.R.id.zzz_ph_notification_image, this.mNotificationImageUrl);
        }

        return this.mExpandedView;
    }

    protected void setTextViewText(int viewId, String text) {
        super.setTextViewText(viewId, text);
        if(this.mExpandedView != null) {
            this.mExpandedView.setTextViewText(viewId, text);
        }

    }

    protected void setViewEnabled(int viewId, boolean enabled) {
        if(this.mExpandedView != null) {
            this.mExpandedView.setBoolean(viewId, "setEnabled", enabled);
        }

        super.setViewEnabled(viewId, enabled);
    }

    protected void setViewVisibility(int viewId, int visible) {
        if(this.mExpandedView != null) {
            this.mExpandedView.setViewVisibility(viewId, visible);
        }

        super.setViewVisibility(viewId, visible);
    }

    protected void setImageViewResource(int viewId, int resourceId) {
        if(this.mExpandedView != null) {
            this.mExpandedView.setImageViewResource(viewId, resourceId);
        }

        super.setImageViewResource(viewId, resourceId);
    }

    protected void setImageViewUri(int viewId, Uri contentUri) {
        super.setImageViewUri(viewId, contentUri);
        if(this.mExpandedView != null && contentUri != null) {
            this.mExpandedView.setImageViewUri(viewId, contentUri);
        }

    }

    protected Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return this.getNotificationBuilder().build();
        } else {
            return this.getNotificationBuilder().getNotification();
        }
    }
}
