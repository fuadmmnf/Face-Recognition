// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.microsoft.projectoxford.face.samples;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.microsoft.projectoxford.face.samples.helper.ImageHelper;
import com.microsoft.projectoxford.face.samples.helper.SampleApp;
import com.microsoft.projectoxford.face.samples.helper.StorageHelper;
import com.microsoft.projectoxford.face.samples.persongroupmanagement.PersonGroupListActivity;
import com.microsoft.projectoxford.face.samples.ui.IdentificationActivity;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Face Detector Demo.
 */
public class FaceDetectionProcessor extends VisionProcessorBase<List<FirebaseVisionFace>> {

    private static final String TAG = "FaceDetectionProcessor";
    private FirebaseVisionImage originalImage;
    public AsyncTask identifyTask, detectTask;
    private final FirebaseVisionFaceDetector detector;
    String mPersonGroupId;
    public Context context;
    public Bitmap mBitmap;
    public String name;
    private TextView list;
    private String student_name = "";
    private List<String> studentList = new ArrayList<>();

    boolean detected;
    FaceListAdapter mFaceListAdapter;
    ImageView imageView;


    public FaceDetectionProcessor(Context context, TextView list, ImageView imageView) {
        this.context = context;
        this.list = list;
        this.imageView = imageView;
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                        .setClassificationType(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                        .setTrackingEnabled(true)
                        .build();

        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage image) {
        originalImage = image;
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(
            @NonNull List<FirebaseVisionFace> faces,
            @NonNull FrameMetadata frameMetadata,
            @NonNull GraphicOverlay graphicOverlay) throws IOException {
        graphicOverlay.clear();
        for (int i = 0; i < faces.size(); ++i) {
            FirebaseVisionFace face = faces.get(i);

            Rect faceBox = face.getBoundingBox();

            Bitmap originalBitmap = originalImage.getBitmapForDebugging();
            int centerX = faceBox.centerX() - faceBox.width() / 2;
            int centerY = faceBox.centerY() - faceBox.height() / 2;

            int width = Math.min(faceBox.width(), originalBitmap.getWidth() - centerX);
            int height = Math.min(faceBox.height(), originalBitmap.getHeight() - centerY);

            Bitmap faceBitmap = Bitmap.createBitmap(originalImage.getBitmapForDebugging(), Math.abs(centerX), Math.abs(centerY), width, height);
            mBitmap = faceBitmap;
            imageView.setImageBitmap(mBitmap);
//            Toast.makeText(context,"start",Toast.LENGTH_LONG).show();
            getGroup();

            detect(mBitmap);


//            Toast.makeText(context, "detect complete", Toast.LENGTH_LONG).show();
            identify();
//            Toast.makeText(context,"identify complete complete",Toast.LENGTH_LONG).show();
            FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay);
            graphicOverlay.add(faceGraphic);
            faceGraphic.updateFace(face, frameMetadata.getCameraFacing(), name);
//            Toast.makeText(context,"end",Toast.LENGTH_LONG).show();

        }
    }

    private void detect(Bitmap bitmap) {
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        // Start a background task to detect faces in the image.
        detectTask=new FaceDetectionProcessor.DetectionTask().execute(inputStream);

    }

    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
//                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        null);
            } catch (Exception e) {
//                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.

        }

        @Override
        protected void onPostExecute(Face[] result) {

            if (result != null) {

                mFaceListAdapter = new FaceDetectionProcessor.FaceListAdapter(result);

                if (result.length == 0) {
                    detected = false;

                } else {
                    detected = true;
                    Toast.makeText(context, "EBAR FACE PAISE", Toast.LENGTH_SHORT).show();
                }
            } else {
                detected = false;
            }

        }
    }

    public void identify() {

        // Start detection task only if the image to detect is selected.
        if (detected && mPersonGroupId != null) {
            // Start a background task to identify faces in the image.
            List<UUID> faceIds = new ArrayList<>();
            for (Face face : mFaceListAdapter.faces) {
                faceIds.add(face.faceId);
            }


            identifyTask = new FaceDetectionProcessor.IdentificationTask(mPersonGroupId).execute(
                    faceIds.toArray(new UUID[faceIds.size()]));




        } else {
            // Not detected or person group exists.

        }
    }

    private class IdentificationTask extends AsyncTask<UUID, String, IdentifyResult[]> {
        private boolean mSucceed = true;
        String mPersonGroupId;

        IdentificationTask(String personGroupId) {
            this.mPersonGroupId = personGroupId;
        }

        @Override
        protected IdentifyResult[] doInBackground(UUID... params) {
            String logString = "Request: Identifying faces ";
            for (UUID faceId : params) {
                logString += faceId.toString() + ", ";
            }
            logString += " in group " + mPersonGroupId;

            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
//                publishProgress("Getting person group status...");

                TrainingStatus trainingStatus = faceServiceClient.getLargePersonGroupTrainingStatus(
                        this.mPersonGroupId);     /* personGroupId */
                if (trainingStatus.status != TrainingStatus.Status.Succeeded) {
//                    publishProgress("Person group training status is " + trainingStatus.status);
                    mSucceed = false;
                    return null;
                }

//                publishProgress("Identifying...");

                // Start identification.
                return faceServiceClient.identityInLargePersonGroup(
                        this.mPersonGroupId,   /* personGroupId */
                        params,                  /* faceIds */
                        1);  /* maxNumOfCandidatesReturned */
            } catch (Exception e) {
                mSucceed = false;
//                publishProgress(e.getMessage());

                return null;
            }
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.a

        }

        @Override
        protected void onPostExecute(IdentifyResult[] result) {
            // Show the result on screen when detection is done.
            setUiAfterIdentification(result, mSucceed);
            if (result != null) mFaceListAdapter.setIdentificationResult(result);
        }
    }

    private void setUiAfterIdentification(IdentifyResult[] result, boolean succeed) {

        if (succeed) {
            // Set the information about the detection result.


            if (result != null) {
                mFaceListAdapter.setIdentificationResult(result);

                String logString = "Response: Success. ";

                for (IdentifyResult identifyResult : result) {
                    if (identifyResult.candidates.size() > 0) {
                        String personId =
                                identifyResult.candidates.get(0).personId.toString();
                        String personName = StorageHelper.getPersonName(
                                personId, mPersonGroupId, context);
                        name = personName;
                    }

                    Toast.makeText(context, name, Toast.LENGTH_SHORT).show();
                    if (!studentList.contains(name)) {
                        student_name = list.getText() + name;
                        list.setText(student_name);
                        studentList.add(name);
                    }

                    logString += "Face " + identifyResult.faceId.toString() + " is identified as "
                            + (identifyResult.candidates.size() > 0
                            ? name
                            : "Unknown Person")
                            + ". ";
                }


//                Toast.makeText(context,logString,Toast.LENGTH_LONG).show();
            }
        }
    }

    public void getGroup() {


        List<String> personGroupIdList = new ArrayList<>();
        List<Boolean> personGroupChecked = new ArrayList<>();
        Set<String> personGroupIds = StorageHelper.getAllPersonGroupIds(context);
        for (String personGroupId : personGroupIds) {
            personGroupIdList.add(personGroupId);
            personGroupChecked.add(false);
        }
        mPersonGroupId = personGroupIdList.get(0);
        String personGroupName = StorageHelper.getPersonGroupName(
                personGroupIdList.get(0), context);
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }


    private class FaceListAdapter {
        // The detected faces.
        List<Face> faces;
        List<IdentifyResult> mIdentifyResults;
        List<Bitmap> faceThumbnails;

        FaceListAdapter(Face[] detectionResult) {
            faces = new ArrayList<>();
            faceThumbnails = new ArrayList<>();
            mIdentifyResults = new ArrayList<>();

            if (detectionResult != null) {
                faces = Arrays.asList(detectionResult);
                for (Face face : faces) {
                    try {
                        // Crop face thumbnail with five main landmarks drawn from original image.
                        faceThumbnails.add(ImageHelper.generateFaceThumbnail(
                                mBitmap, face.faceRectangle));
                    } catch (IOException e) {

                    }
                }
            }
        }

        public void setIdentificationResult(IdentifyResult[] identifyResults) {
            mIdentifyResults = Arrays.asList(identifyResults);
        }
    }

}
