package com.jarvis.auto;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class AutoTouchService extends AccessibilityService {

    private static AutoTouchService instance;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    public static AutoTouchService getInstance() {
        return instance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    public void clickAt(float x, float y) {
        Path swipePath = new Path();
        swipePath.moveTo(x, y);
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 100));
        dispatchGesture(gestureBuilder.build(), null, null);
    }

    public void scrollDown() {
        Path swipePath = new Path();
        swipePath.moveTo(500, 1500);
        swipePath.lineTo(500, 500);
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 300));
        dispatchGesture(gestureBuilder.build(), null, null);
    }

    public void scrollUp() {
        Path swipePath = new Path();
        swipePath.moveTo(500, 500);
        swipePath.lineTo(500, 1500);
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 300));
        dispatchGesture(gestureBuilder.build(), null, null);
    }
}
