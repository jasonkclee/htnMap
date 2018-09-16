/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2basic;

import android.app.ActionBar;
import android.graphics.Matrix;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.graphics.Matrix;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.graphics.Camera;

public class CameraActivity extends AppCompatActivity {
    Camera mCamera = new Camera();
    private Matrix getTransformationMatrix() {
        Matrix matrix = new Matrix();
        mCamera.save();
        mCamera.translate(0, 0, 4);
        mCamera.getMatrix(matrix);
        mCamera.restore();

        return matrix;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (null == savedInstanceState) {
            // add the image
            RelativeLayout relativeLayout = findViewById(R.id.relative_container);
            ImageView arrowImage = new ImageView(this);
            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
            arrowImage.setImageResource(R.drawable.arrow);
            relativeLayout
                    .addView(arrowImage, params);

            // rotate the image
            Matrix matrix = arrowImage.getMatrix();
            arrowImage.setScaleType(ImageView.ScaleType.MATRIX);
            arrowImage.setImageMatrix(getTransformationMatrix());
        }

    }


}
