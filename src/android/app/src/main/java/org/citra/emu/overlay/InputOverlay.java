package org.citra.emu.overlay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.preference.PreferenceManager;

import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Arrays;

import org.citra.emu.NativeLibrary.ButtonType;
import org.citra.emu.R;
import org.citra.emu.ui.EmulationActivity;

public final class InputOverlay extends View {
    public static final String PREF_CONTROLLER_INIT = "InitOverlay";
    public static final String PREF_HAPTIC_FEEDBACK = "UseHapticFeedback";
    public static final String PREF_JOYSTICK_RELATIVE = "JoystickRelative";
    public static final String PREF_CONTROLLER_SCALE = "ControllerScale";
    public static final String PREF_CONTROLLER_ALPHA = "ControllerAlpha";
    public static final String PREF_CONTROLLER_HIDE = "ControllerHide";
    public static final String PREF_SHOW_RIGHT_JOYSTICK = "ShowRightJoystick";

    public static int sControllerScale = 50;
    public static int sControllerAlpha = 100;
    public static boolean sJoystickRelative = true;
    public static boolean sHideInputOverlay = false;
    public static boolean sShowRightJoystick = false;
    public static boolean sUseHapticFeedback = false;

    private final ArrayList<InputOverlayButton> mButtons = new ArrayList<>();
    private final ArrayList<InputOverlayDpad> mDpads = new ArrayList<>();
    private final ArrayList<InputOverlayJoystick> mJoysticks = new ArrayList<>();
    private boolean mIsLandscape = false;
    private boolean mInEditMode = false;
    private InputOverlayButton mButtonBeingConfigured;
    private InputOverlayDpad mDpadBeingConfigured;
    private InputOverlayJoystick mJoystickBeingConfigured;
    private InputOverlayPointer mOverlayPointer;
    private Paint mPaint;

    private SharedPreferences mPreferences;

    public InputOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!mPreferences.getBoolean(PREF_CONTROLLER_INIT, false)) {
            defaultOverlay();
        }
        sUseHapticFeedback = mPreferences.getBoolean(InputOverlay.PREF_HAPTIC_FEEDBACK, true);
        sJoystickRelative = mPreferences.getBoolean(InputOverlay.PREF_JOYSTICK_RELATIVE, true);
        sControllerScale = mPreferences.getInt(InputOverlay.PREF_CONTROLLER_SCALE, 50);
        sControllerAlpha = mPreferences.getInt(InputOverlay.PREF_CONTROLLER_ALPHA, 100);
        sHideInputOverlay = mPreferences.getBoolean(InputOverlay.PREF_CONTROLLER_HIDE, false);
        sShowRightJoystick = mPreferences.getBoolean(InputOverlay.PREF_SHOW_RIGHT_JOYSTICK, false);
    }

    private void defaultOverlay() {
        SharedPreferences.Editor sPrefsEditor = mPreferences.edit();
        Resources res = getResources();
        int[][] buttons = new int[][] {
            {ButtonType.N3DS_BUTTON_A, R.integer.BUTTON_A_X, R.integer.BUTTON_A_Y},
            {ButtonType.N3DS_BUTTON_B, R.integer.BUTTON_B_X, R.integer.BUTTON_B_Y},
            {ButtonType.N3DS_BUTTON_X, R.integer.BUTTON_X_X, R.integer.BUTTON_X_Y},
            {ButtonType.N3DS_BUTTON_Y, R.integer.BUTTON_Y_X, R.integer.BUTTON_Y_Y},
            {ButtonType.N3DS_BUTTON_START, R.integer.BUTTON_START_X, R.integer.BUTTON_START_Y},
            {ButtonType.N3DS_BUTTON_SELECT, R.integer.BUTTON_SELECT_X, R.integer.BUTTON_SELECT_Y},
            {ButtonType.N3DS_BUTTON_ZL, R.integer.TRIGGER_ZL_X, R.integer.TRIGGER_ZL_Y},
            {ButtonType.N3DS_BUTTON_ZR, R.integer.TRIGGER_ZR_X, R.integer.TRIGGER_ZR_Y},
            {ButtonType.N3DS_BUTTON_L, R.integer.TRIGGER_L_X, R.integer.TRIGGER_L_Y},
            {ButtonType.N3DS_BUTTON_R, R.integer.TRIGGER_R_X, R.integer.TRIGGER_R_Y},
            {ButtonType.N3DS_DPAD_UP, R.integer.PAD_MAIN_X, R.integer.PAD_MAIN_Y},
            {ButtonType.N3DS_CPAD_X, R.integer.STICK_MAIN_X, R.integer.STICK_MAIN_Y},
            {ButtonType.N3DS_STICK_X, R.integer.STICK_RIGHT_X, R.integer.STICK_RIGHT_Y},
            {ButtonType.EMU_COMBO_KEY_1, R.integer.COMBO_KEY1_X, R.integer.COMBO_KEY1_Y},
            {ButtonType.EMU_COMBO_KEY_2, R.integer.COMBO_KEY2_X, R.integer.COMBO_KEY2_Y},
            {ButtonType.EMU_COMBO_KEY_3, R.integer.COMBO_KEY3_X, R.integer.COMBO_KEY3_Y},
        };

        for (int[] button : buttons) {
            int id = button[0];
            int x = button[1];
            int y = button[2];
            float posX = res.getInteger(x) / 100.0f;
            float posY = res.getInteger(y) / 100.0f;
            sPrefsEditor.putFloat(id + "_X", posX);
            sPrefsEditor.putFloat(id + "_Y", posY);
            sPrefsEditor.putFloat(id + "_XX", posX);
            sPrefsEditor.putFloat(id + "_YY", posY);
        }

        sPrefsEditor.putBoolean(PREF_CONTROLLER_INIT, true);
        sPrefsEditor.apply();
    }

    @Override
    public void onDraw(Canvas canvas) {
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(mPaint);

        mPaint.setAlpha((sControllerAlpha * 255) / 100);

        for (InputOverlayButton button : mButtons) {
            button.onDraw(canvas, mPaint);
        }

        for (InputOverlayDpad dpad : mDpads) {
            dpad.onDraw(canvas, mPaint);
        }

        for (InputOverlayJoystick joystick : mJoysticks) {
            joystick.onDraw(canvas, mPaint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isInEditMode()) {
            return onTouchWhileEditing(event);
        }

        boolean isPressed = false;
        boolean isProcessed = false;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN: {
            int pointerIndex = event.getActionIndex();
            int pointerId = event.getPointerId(pointerIndex);
            float pointerX = event.getX(pointerIndex);
            float pointerY = event.getY(pointerIndex);

            for (InputOverlayJoystick joystick : mJoysticks) {
                if (joystick.getBounds().contains((int)pointerX, (int)pointerY)) {
                    joystick.onPointerDown(pointerId, pointerX, pointerY);
                    isProcessed = true;
                    break;
                }
            }

            for (InputOverlayButton button : mButtons) {
                if (button.getBounds().contains((int)pointerX, (int)pointerY)) {
                    button.onPointerDown(pointerId, pointerX, pointerY);
                    isPressed = true;
                    isProcessed = true;
                }
            }

            for (InputOverlayDpad dpad : mDpads) {
                if (dpad.getBounds().contains((int)pointerX, (int)pointerY)) {
                    dpad.onPointerDown(pointerId, pointerX, pointerY);
                    isPressed = true;
                    isProcessed = true;
                }
            }

            if (!isProcessed && mOverlayPointer != null && mOverlayPointer.getPointerId() == -1)
                mOverlayPointer.onPointerDown(pointerId, pointerX, pointerY);
            break;
        }
        case MotionEvent.ACTION_MOVE: {
            int pointerCount = event.getPointerCount();
            for (int i = 0; i < pointerCount; ++i) {
                boolean isCaptured = false;
                int pointerId = event.getPointerId(i);
                float pointerX = event.getX(i);
                float pointerY = event.getY(i);

                for (InputOverlayJoystick joystick : mJoysticks) {
                    if (joystick.getPointerId() == pointerId) {
                        joystick.onPointerMove(pointerId, pointerX, pointerY);
                        isCaptured = true;
                        isProcessed = true;
                        break;
                    }
                }
                if (isCaptured)
                    continue;

                for (InputOverlayButton button : mButtons) {
                    if (button.getBounds().contains((int)pointerX, (int)pointerY)) {
                        if (button.getPointerId() == -1) {
                            button.onPointerDown(pointerId, pointerX, pointerY);
                            isPressed = true;
                            isProcessed = true;
                        }
                    } else if (button.getPointerId() == pointerId) {
                        button.onPointerUp(pointerId, pointerX, pointerY);
                        isProcessed = true;
                    }
                }

                for (InputOverlayDpad dpad : mDpads) {
                    if (dpad.getPointerId() == pointerId) {
                        dpad.onPointerMove(pointerId, pointerX, pointerY);
                        if (!isPressed && dpad.isPressed()) {
                            isPressed = true;
                        }
                        isProcessed = true;
                    }
                }

                if (mOverlayPointer != null && mOverlayPointer.getPointerId() == pointerId) {
                    mOverlayPointer.onPointerMove(pointerId, pointerX, pointerY);
                }
            }
            break;
        }

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP: {
            int pointerIndex = event.getActionIndex();
            int pointerId = event.getPointerId(pointerIndex);
            float pointerX = event.getX(pointerIndex);
            float pointerY = event.getY(pointerIndex);

            if (mOverlayPointer != null && mOverlayPointer.getPointerId() == pointerId) {
                mOverlayPointer.onPointerUp(pointerId, pointerX, pointerY);
            }

            for (InputOverlayJoystick joystick : mJoysticks) {
                if (joystick.getPointerId() == pointerId) {
                    joystick.onPointerUp(pointerId, pointerX, pointerY);
                    isProcessed = true;
                    break;
                }
            }

            for (InputOverlayButton button : mButtons) {
                if (button.getPointerId() == pointerId) {
                    button.onPointerUp(pointerId, pointerX, pointerY);
                    isProcessed = true;
                }
            }

            for (InputOverlayDpad dpad : mDpads) {
                if (dpad.getPointerId() == pointerId) {
                    dpad.onPointerUp(pointerId, pointerX, pointerY);
                    isProcessed = true;
                }
            }
            break;
        }

        case MotionEvent.ACTION_CANCEL: {
            isProcessed = true;
            if (mOverlayPointer != null) {
                mOverlayPointer.onPointerUp(0, 0, 0);
            }

            for (InputOverlayJoystick joystick : mJoysticks) {
                joystick.onPointerUp(0, 0, 0);
            }

            for (InputOverlayButton button : mButtons) {
                button.onPointerUp(0, 0, 0);
            }

            for (InputOverlayDpad dpad : mDpads) {
                dpad.onPointerUp(0, 0, 0);
            }
            break;
        }
        }

        if (isProcessed) {
            invalidate();
            if (isPressed) {
                onPressedFeedback();
            }
        }

        return true;
    }

    public boolean onTouchWhileEditing(MotionEvent event) {
        int pointerIndex = event.getActionIndex();
        int pointerX = (int)event.getX(pointerIndex);
        int pointerY = (int)event.getY(pointerIndex);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN:
            if (mButtonBeingConfigured != null || mDpadBeingConfigured != null ||
                mJoystickBeingConfigured != null)
                return false;
            for (InputOverlayButton button : mButtons) {
                if (mButtonBeingConfigured == null &&
                    button.getBounds().contains(pointerX, pointerY)) {
                    mButtonBeingConfigured = button;
                    mButtonBeingConfigured.onConfigureBegin(pointerX, pointerY);
                    return true;
                }
            }

            for (InputOverlayDpad dpad : mDpads) {
                if (mDpadBeingConfigured == null && dpad.getBounds().contains(pointerX, pointerY)) {
                    mDpadBeingConfigured = dpad;
                    mDpadBeingConfigured.onConfigureBegin(pointerX, pointerY);
                    return true;
                }
            }

            for (InputOverlayJoystick joystick : mJoysticks) {
                if (mJoystickBeingConfigured == null &&
                    joystick.getBounds().contains(pointerX, pointerY)) {
                    mJoystickBeingConfigured = joystick;
                    mJoystickBeingConfigured.onConfigureBegin(pointerX, pointerY);
                    return true;
                }
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (mButtonBeingConfigured != null) {
                mButtonBeingConfigured.onConfigureMove(pointerX, pointerY);
                invalidate();
                return true;
            }
            if (mDpadBeingConfigured != null) {
                mDpadBeingConfigured.onConfigureMove(pointerX, pointerY);
                invalidate();
                return true;
            }
            if (mJoystickBeingConfigured != null) {
                mJoystickBeingConfigured.onConfigureMove(pointerX, pointerY);
                invalidate();
                return true;
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
            if (mButtonBeingConfigured != null) {
                saveControlPosition(mButtonBeingConfigured.getButtonId(),
                                    mButtonBeingConfigured.getBounds());
                mButtonBeingConfigured = null;
                return true;
            }
            if (mDpadBeingConfigured != null) {
                saveControlPosition(mDpadBeingConfigured.getButtonId(),
                                    mDpadBeingConfigured.getBounds());
                mDpadBeingConfigured = null;
                return true;
            }
            if (mJoystickBeingConfigured != null) {
                saveControlPosition(mJoystickBeingConfigured.getButtonId(),
                                    mJoystickBeingConfigured.getBounds());
                mJoystickBeingConfigured = null;
                return true;
            }
            break;
        }

        return false;
    }

    private void saveControlPosition(int buttonId, Rect bounds) {
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        SharedPreferences.Editor sPrefsEditor = mPreferences.edit();
        float x =
            (bounds.left + (bounds.right - bounds.left) / 2.0f) / dm.widthPixels * 2.0f - 1.0f;
        float y =
            (bounds.top + (bounds.bottom - bounds.top) / 2.0f) / dm.heightPixels * 2.0f - 1.0f;
        sPrefsEditor.putFloat(buttonId + (mIsLandscape ? "_XX" : "_X"), x);
        sPrefsEditor.putFloat(buttonId + (mIsLandscape ? "_YY" : "_Y"), y);
        sPrefsEditor.apply();
    }

    public void refreshControls() {
        // Remove all the overlay buttons
        mIsLandscape =
            getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        mButtons.clear();
        mDpads.clear();
        mJoysticks.clear();
        mOverlayPointer = new InputOverlayPointer();

        if (sHideInputOverlay)
            return;

        int[][] buttons = new int[][] {
            {ButtonType.N3DS_BUTTON_A, R.drawable.a, R.drawable.a_pressed},
            {ButtonType.N3DS_BUTTON_B, R.drawable.b, R.drawable.b_pressed},
            {ButtonType.N3DS_BUTTON_X, R.drawable.x, R.drawable.x_pressed},
            {ButtonType.N3DS_BUTTON_Y, R.drawable.y, R.drawable.y_pressed},
            {ButtonType.N3DS_BUTTON_START, R.drawable.start, R.drawable.start_pressed},
            {ButtonType.N3DS_BUTTON_SELECT, R.drawable.select, R.drawable.select_pressed},
            {ButtonType.N3DS_BUTTON_L, R.drawable.l, R.drawable.l_pressed},
            {ButtonType.N3DS_BUTTON_R, R.drawable.r, R.drawable.r_pressed},
            {ButtonType.N3DS_BUTTON_ZL, R.drawable.zl, R.drawable.zl_pressed},
            {ButtonType.N3DS_BUTTON_ZR, R.drawable.zr, R.drawable.zr_pressed},
        };
        for (int i = 0; i < buttons.length; ++i) {
            int id = buttons[i][0];
            int normal = buttons[i][1];
            int pressed = buttons[i][2];
            int[] buttonIds = {id};
            mButtons.add(initializeButton(normal, pressed, id, buttonIds));
        }

        int[][] combokeys = {
                {ButtonType.EMU_COMBO_KEY_1, R.drawable.one, R.drawable.one_pressed},
                {ButtonType.EMU_COMBO_KEY_2, R.drawable.two, R.drawable.two_pressed},
                {ButtonType.EMU_COMBO_KEY_3, R.drawable.three, R.drawable.three_pressed},
        };
        for (int i = 0; i < combokeys.length; ++i) {
            String value = mPreferences.getString("combo_key_" + i, "");
            String[] keyStrs = value.split(",");
            ArrayList<Integer> keys = new ArrayList<>();
            for (String key: keyStrs) {
                if (!key.isEmpty()) {
                    keys.add(Integer.parseInt(key));
                }
            }
            if (keys.size() > 0) {
                int id = combokeys[i][0];
                int normal = combokeys[i][1];
                int pressed = combokeys[i][2];
                int[] buttonIds = new int[keys.size()];
                Arrays.setAll(buttonIds, keys::get);
                mButtons.add(initializeButton(normal, pressed, id, buttonIds));
            }
        }

        // mDpads.add(initializeDpad(ButtonType.N3DS_CPAD_X));
        mDpads.add(initializeDpad(ButtonType.N3DS_DPAD_UP));
        mJoysticks.add(initializeJoystick(ButtonType.N3DS_CPAD_X));
        if (sShowRightJoystick) {
            mJoysticks.add(initializeJoystick(ButtonType.N3DS_STICK_X));
        }
    }

    private InputOverlayButton initializeButton(int defaultResId, int pressedResId, int id, int[] buttons) {
        final Resources res = getResources();
        final DisplayMetrics dm = res.getDisplayMetrics();
        float scale = 0.14f * (sControllerScale + 50) / 100;

        switch (id) {
        case ButtonType.N3DS_BUTTON_L:
        case ButtonType.N3DS_BUTTON_R:
            scale *= 1.7f;
            break;
        case ButtonType.N3DS_BUTTON_ZL:
        case ButtonType.N3DS_BUTTON_ZR:
            scale *= 1.2f;
            break;

        case ButtonType.N3DS_BUTTON_START:
        case ButtonType.N3DS_BUTTON_SELECT:
            scale *= 0.8f;
            break;

        case ButtonType.EMU_COMBO_KEY_1:
        case ButtonType.EMU_COMBO_KEY_2:
        case ButtonType.EMU_COMBO_KEY_3:
            scale *= 0.9f;
            break;
        }

        Bitmap defaultBitmap = getInputBitmap(defaultResId, scale);
        Bitmap pressedBitmap = getInputBitmap(pressedResId, scale);
        InputOverlayButton overlay =
            new InputOverlayButton(defaultBitmap, pressedBitmap, id, buttons);

        // The X and Y coordinates of the InputOverlayDrawableButton on the InputOverlay.
        // These were set in the input overlay configuration menu.
        float x = mPreferences.getFloat(id + (mIsLandscape ? "_XX" : "_X"), 0f);
        float y = mPreferences.getFloat(id + (mIsLandscape ? "_YY" : "_Y"), 0.5f);

        int width = defaultBitmap.getWidth();
        int height = defaultBitmap.getHeight();
        int drawableX = (int)((dm.widthPixels / 2.0f) * (1.0f + x) - width / 2.0f);
        int drawableY = (int)((dm.heightPixels / 2.0f) * (1.0f + y) - height / 2.0f);
        // Now set the bounds for the InputOverlayDrawableButton.
        // This will dictate where on the screen (and the what the size) the
        // InputOverlayDrawableButton will be.
        overlay.setBounds(new Rect(drawableX, drawableY, drawableX + width, drawableY + height));

        return overlay;
    }

    private InputOverlayDpad initializeDpad(int dpadId) {
        final Resources res = getResources();
        final DisplayMetrics dm = res.getDisplayMetrics();
        final int defaultResId = R.drawable.dpad;
        final int pressedOneDirectionResId = R.drawable.dpad_pressed_one;
        final int pressedTwoDirectionsResId = R.drawable.dpad_pressed_two;
        float scale = 0.32f * (sControllerScale + 50) / 100;

        Bitmap defaultBitmap = getInputBitmap(defaultResId, scale);
        Bitmap onePressedBitmap = getInputBitmap(pressedOneDirectionResId, scale);
        Bitmap twoPressedBitmap = getInputBitmap(pressedTwoDirectionsResId, scale);
        InputOverlayDpad overlay =
                new InputOverlayDpad(defaultBitmap, onePressedBitmap, twoPressedBitmap, dpadId);

        // The X and Y coordinates of the InputOverlayDrawableDpad on the InputOverlay.
        // These were set in the input overlay configuration menu.
        float x = mPreferences.getFloat(dpadId + (mIsLandscape ? "_XX" : "_X"), 0f);
        float y = mPreferences.getFloat(dpadId + (mIsLandscape ? "_YY" : "_Y"), 0.5f);

        int width = defaultBitmap.getWidth();
        int height = defaultBitmap.getHeight();
        int drawableX = (int)((dm.widthPixels / 2.0f) * (1.0f + x) - width / 2.0f);
        int drawableY = (int)((dm.heightPixels / 2.0f) * (1.0f + y) - height / 2.0f);
        // Now set the bounds for the InputOverlayDrawableDpad.
        // This will dictate where on the screen (and the what the size) the
        // InputOverlayDrawableDpad will be.
        overlay.setBounds(new Rect(drawableX, drawableY, drawableX + width, drawableY + height));

        return overlay;
    }

    private InputOverlayJoystick initializeJoystick(int joystick) {
        final Resources res = getResources();
        final DisplayMetrics dm = res.getDisplayMetrics();
        int resOuter = R.drawable.joystick_range;
        int defaultResInner = R.drawable.joystick;
        int pressedResInner = R.drawable.joystick_pressed;
        float scale = 0.275f * (sControllerScale + 50) / 100;

        if (joystick == ButtonType.N3DS_STICK_X) {
            resOuter = R.drawable.c_stick_range;
            defaultResInner = R.drawable.c_stick;
            pressedResInner = R.drawable.c_stick_pressed;
        }

        // Initialize the InputOverlayDrawableJoystick.
        final Bitmap bitmapOuter = getInputBitmap(resOuter, scale);
        final Bitmap bitmapInnerDefault = getInputBitmap(defaultResInner, 1.0f);
        final Bitmap bitmapInnerPressed = getInputBitmap(pressedResInner, 1.0f);

        // The X and Y coordinates of the InputOverlayDrawableButton on the InputOverlay.
        // These were set in the input overlay configuration menu.
        float x = mPreferences.getFloat(joystick + (mIsLandscape ? "_XX" : "_X"), -0.3f);
        float y = mPreferences.getFloat(joystick + (mIsLandscape ? "_YY" : "_Y"), 0.3f);

        // Now set the bounds for the InputOverlayDrawableJoystick.
        // This will dictate where on the screen (and the what the size) the
        // InputOverlayDrawableJoystick will be.
        float innerScale = 1.7f;
        int outerSize = bitmapOuter.getWidth();
        int drawableX = (int)((dm.widthPixels / 2.0f) * (1.0f + x) - outerSize / 2.0f);
        int drawableY = (int)((dm.heightPixels / 2.0f) * (1.0f + y) - outerSize / 2.0f);
        Rect outerRect =
            new Rect(drawableX, drawableY, drawableX + outerSize, drawableY + outerSize);
        Rect innerRect =
            new Rect(0, 0, (int)(outerSize / innerScale), (int)(outerSize / innerScale));

        // Send the drawableId to the joystick so it can be referenced when saving control position.
        InputOverlayJoystick overlay = new InputOverlayJoystick(
            bitmapOuter, bitmapInnerDefault, bitmapInnerPressed, outerRect, innerRect, joystick);

        return overlay;
    }

    public void onPressedFeedback() {
        if (sUseHapticFeedback) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    public Bitmap getInputBitmap(int id, float scale) {
        return ((EmulationActivity)getContext()).getInputBitmap(id, scale);
    }

    public boolean isInEditMode() {
        return mInEditMode;
    }

    public void setInEditMode(boolean mode) {
        mInEditMode = mode;
    }

    public static final int[] ResIds = {
            R.drawable.a, R.drawable.a_pressed,
            R.drawable.b, R.drawable.b_pressed,
            R.drawable.x, R.drawable.x_pressed,
            R.drawable.y, R.drawable.y_pressed,
            R.drawable.l, R.drawable.l_pressed,
            R.drawable.r, R.drawable.r_pressed,
            R.drawable.zl, R.drawable.zl_pressed,
            R.drawable.zr, R.drawable.zr_pressed,
            R.drawable.start, R.drawable.start_pressed,
            R.drawable.select, R.drawable.select_pressed,
            R.drawable.one, R.drawable.one_pressed,
            R.drawable.two, R.drawable.two_pressed,
            R.drawable.three, R.drawable.three_pressed,
            R.drawable.dpad, R.drawable.dpad_pressed_one, R.drawable.dpad_pressed_two,
            R.drawable.joystick, R.drawable.joystick_pressed, R.drawable.joystick_range,
            R.drawable.c_stick, R.drawable.c_stick_pressed, R.drawable.c_stick_range,
            R.drawable.bg_landscape, R.drawable.bg_portrait
    };
    public static final String[] ResNames = {
            "a.png", "a_pressed.png",
            "b.png", "b_pressed.png",
            "x.png", "x_pressed.png",
            "y.png", "y_pressed.png",
            "l.png", "l_pressed.png",
            "r.png", "r_pressed.png",
            "zl.png", "zl_pressed.png",
            "zr.png", "zr_pressed.png",
            "start.png", "start_pressed.png",
            "select.png", "select_pressed.png",
            "one.png", "one_pressed.png",
            "two.png", "two_pressed.png",
            "three.png", "three_pressed.png",
            "dpad.png", "dpad_pressed_one.png", "dpad_pressed_two.png",
            "joystick.png", "joystick_pressed.png", "joystick_range.png",
            "c_stick.png", "c_stick_pressed.png", "c_stick_range.png",
            "bg_landscape.jpg", "bg_portrait.jpg"
    };
}