package com.example.capstonefinaldiary;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.capstonefinaldiary.Models.AudioFileInfo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class RecordActivity extends AppCompatActivity {

    /**xml 변수*/
    private ImageButton audioRecordImageBtn, audioStopImageBtn, voicelib;
    private TextView audioRecordText, timeText;
    private Button saveButton;
    private Button reRecordButton;
    // 오디오 권한
    private String recordPermission = Manifest.permission.RECORD_AUDIO;
    private int PERMISSION_CODE = 21;
    // 녹음 진행 시간 관련 변수
    private int seconds = 0;
    private Handler handler = new Handler();
    // 오디오 파일 녹음 관련 변수
    private MediaRecorder mediaRecorder;
    private String audioFileName;
    private boolean isRecording = false;
    private boolean isPaused = false; // 녹음이 중지된 상태인지 여부를 나타내는 플래그
    private Uri audioUri = null; // 오디오 파일 uri
    //private ArrayList<Uri> recordedUris = new ArrayList<>();
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference audioRef;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        // Firebase SDK 초기화
        FirebaseApp.initializeApp(this);

        // Firebase Realtime Database 레퍼런스 초기화
        databaseReference = FirebaseDatabase.getInstance().getReference("audios");

        // 권한 확인 및 요청
        if (checkAudioPermission()) {
            init();
        }
    }
    private void init() {
        audioRecordImageBtn = findViewById(R.id.audioRecordImageBtn);
        audioStopImageBtn = findViewById(R.id.audioStopImageBtn);
        audioRecordText = findViewById(R.id.audioRecordText);
        voicelib = findViewById(R.id.voicelib);
        saveButton = findViewById(R.id.saveButton);
        reRecordButton = findViewById(R.id.reRecordButton);
        timeText = findViewById(R.id.timeText);


        // Firebase Storage 인스턴스 가져오기
        storage = FirebaseStorage.getInstance("gs://finalcapstone-749d2.appspot.com");

        // 재생버튼 클릭
        audioRecordImageBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isRecording){
                    // 녹음이 시작되지 않은 경우
                    if (isPaused) {
                        // 일시 정지 상태라면 재개
                        resumeRecording();
                    } else {
                        if (checkAudioPermission()) {
                            // 새로운 녹음 시작
                            startRecording();
                        }
                    }
                }else{
                    //녹음 중인 경우
                    stopRecording();
                }
            }
        });

        // 중지버튼 클릭
        audioStopImageBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view){
                stopRecording();
            }
        });

        // 저장버튼 클릭이벤트
        saveButton.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View view) {
                saveRecording();
            }
        });

        // 재녹음버튼 클릭이벤트
        reRecordButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                reRecording();
            }
        });


        // 녹음파일 인턴트 이동
        voicelib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Voicelib 버튼이 클릭되었을 때, AudioFileActivity로 전환
                Intent intent = new Intent(RecordActivity.this, AudioFileActivity.class);
                /**
                if(recordedUris != null){
                    // recordedUris 리스트를 Intent에 추가
                    intent.putParcelableArrayListExtra("recordedUris", recordedUris);
                }
                 */
                startActivity(intent);
            }
        });

    }


    // 오디오 파일 권한 체크
    private boolean checkAudioPermission() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), recordPermission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{recordPermission}, PERMISSION_CODE);
            return false;
        }
    }

    // 녹음 시작
    private void startRecording() {
        if (!isRecording) {
            seconds = 0; // 새로운 녹음이 시작될 때 시간 초기화
            // 녹음 시간 갱신을 위한 Runnable 시작
            handler.post(updateTimer);
            // 이전 녹음 세션을 릴리스
            if (mediaRecorder != null) {
                mediaRecorder.release();
                mediaRecorder = null;
            }

            mediaRecorder = new MediaRecorder();
            mediaRecorder.reset();

            // 처음 녹음을 시작하는 경우, 새로운 파일을 생성합니다.
            // 파일 경로 설정
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:ss", Locale.getDefault());
            String currentTime = sdf.format(new Date());

            audioFileName = getExternalCacheDir().getAbsolutePath() + "/Record_" + currentTime + ".aac";

            // Firebase Storage 루트 경로 설정 (수정)
            storageRef = storage.getReference().child("audio");
            //storageRef = storage.getReferenceFromUrl("gs://finalcapstone-749d2.appspot.com").child(audioFileName);

            // 디렉토리 위치 확인
            Log.d("audioFilePath", audioFileName);

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // 마이크 사용하여 입력
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // 녹음된 오디로 파일의 출력형식 지정
            mediaRecorder.setOutputFile(audioFileName); // Firebase Storage 경로로 설정
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); // 녹음된 오디로 인코딩 형식 설정

            try {
                // mediaRecorder 객체 초기화
                mediaRecorder.prepare();
            }catch (IOException e) {
                e.printStackTrace();
            }

            if (mediaRecorder != null) {
                mediaRecorder.start();
            }

            audioRecordText.setText("녹음중");

            // 재생버튼을 눌렀을 때 버튼들의 가시성
            audioRecordImageBtn.setVisibility(View.INVISIBLE);
            audioStopImageBtn.setVisibility(View.VISIBLE);
            saveButton.setVisibility(View.INVISIBLE);
            reRecordButton.setVisibility(View.INVISIBLE);
            timeText.setVisibility(View.VISIBLE);

            isRecording = true;
        }
    }

    // 녹음 재개
    private void resumeRecording(){
        if (mediaRecorder != null) {
            mediaRecorder.resume();
            isPaused = false;
            isRecording = true;
        }

        audioRecordText.setText("녹음중");
        // 재생버튼을 눌렀을 때 버튼들의 가시성
        audioRecordImageBtn.setVisibility(View.INVISIBLE);
        audioStopImageBtn.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.INVISIBLE);
        reRecordButton.setVisibility(View.INVISIBLE);
        timeText.setVisibility(View.VISIBLE);

        // 녹음 시간 갱신을 다시 시작합니다.
        handler.post(updateTimer);
    }

    // 녹음 일시 중지
    private void stopRecording() {
        if (mediaRecorder != null) {
            // 녹음 중지
            mediaRecorder.pause();
        }

        isRecording = false;
        isPaused = true;

        // 녹음이 중지될 때 버튼 가시성 설정
        audioRecordImageBtn.setVisibility(View.VISIBLE);
        audioStopImageBtn.setVisibility(View.INVISIBLE);
        saveButton.setVisibility(View.VISIBLE);
        reRecordButton.setVisibility(View.VISIBLE);
        timeText.setVisibility(View.VISIBLE);
        audioRecordText.setText("버튼을 누르면 녹음이 재개됩니다.");
        Toast.makeText(RecordActivity.this, "녹음 파일을 저장하려면 저장버튼을 누르세요.", Toast.LENGTH_SHORT).show();

        // 녹음 시간 갱신을 멈춥니다.
        handler.removeCallbacks(updateTimer);
    }

    // 녹음 종료(저장)
    private void saveRecording() {
        if (!isPaused) {
            Toast.makeText(RecordActivity.this, "녹음을 일시정지 한 후 저장해주세요.", Toast.LENGTH_SHORT).show();
        } else {
            // 녹음이 중지된 상태라면
            if (mediaRecorder != null) {
                // mediaRecorder를 해제하고 파일을 저장
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;

                // 녹음 시간 갱신을 위한 Runnable 종료
                handler.removeCallbacks(updateTimer);
            }

            if (audioFileName != null) {
                audioUri = Uri.fromFile(new File(audioFileName));
                audioRef = storageRef.child(audioUri.getLastPathSegment()); // 가장 최근 생성된 파일로 Firebase Storage 경로 수정
                // MIME 유형을 "audio/aac"으로 설정
                StorageMetadata metadata = new StorageMetadata.Builder()
                        .setContentType("audio/aac")
                        .build();
                audioRef.putFile(audioUri, metadata)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // 파일 업로드 성공
                                audioRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri downloadUri) {
                                        // 업로드된 파일의 다운로드 URL을 얻음
                                        String audioUri = downloadUri.toString();
                                        // 업로드된 파일의 URL을 사용하거나 저장할 수 있습니다.
                                        // 예를 들어 Firebase Realtime Database에 저장할 수 있습니다.
                                        saveAudioFileInfoToDatabase(audioFileName, audioUri); // Firebase Realtime Database에 오디오 파일 정보 저장
                                    }
                                });

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // 파일 업로드 실패
                                // ... 실패 시 처리
                                Log.d("MyApp","upload Failed.");
                            }
                        });

                Toast.makeText(RecordActivity.this, "녹음 파일이 저장되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RecordActivity.this, "녹음 파일이 없습니다.", Toast.LENGTH_SHORT).show();
            }

            // UI 변경
            audioRecordText.setText("버튼을 누르면 녹음이 시작됩니다.");
            audioRecordImageBtn.setVisibility(View.VISIBLE);
            audioStopImageBtn.setVisibility(View.INVISIBLE);
            saveButton.setVisibility(View.INVISIBLE);
            reRecordButton.setVisibility(View.INVISIBLE);
            timeText.setVisibility(View.INVISIBLE);

            // 녹음상태 초기화 (새로운 녹음을 시작하기 위해)
            isRecording = false;
            isPaused = false;
        }
    }
    // 재녹음
    private void reRecording(){
        if(!isPaused){
            Toast.makeText(RecordActivity.this, "녹음을 일시정지 한 후 재녹음 해주세요.", Toast.LENGTH_SHORT).show();
        }else{
            // 이전 녹음 파일 삭제
            deletePreviousRecording();

            // mediaRecorder를 해제하고 null로 설정
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            // 새로운 녹음 시작
            if (checkAudioPermission()) {
                startRecording();
            }
        }
    }
    // 녹음 시간을 갱신하는 Runnable
    private Runnable updateTimer = new Runnable() {
        public void run() {
            if (isRecording) {
                seconds++;
                int minutes = seconds / 60;
                int remainderSeconds = seconds % 60;
                timeText.setText(String.format("%02d:%02d", minutes, remainderSeconds));
                handler.postDelayed(this, 1000); // 1초마다 갱신
            }
        }
    };

    // 이전 녹음된 파일 삭제
    private void deletePreviousRecording() {
        if (audioFileName != null) {
            File file = new File(audioFileName);
            if (file.exists()) {
                file.delete();
            }
            audioFileName = null;
        }
    }
    // Firebase Realtime Database에 오디오 파일 정보 저장
    private void saveAudioFileInfoToDatabase(String filename, String url) {
        // 오디오 파일 정보를 Firebase Realtime Database에 저장
        // "audios" 노드 아래에 새로운 노드 생성
        String key = databaseReference.push().getKey();

        // JSON 형식으로 데이터 생성
        Map<String, Object> audioInfo = new HashMap<>();
        audioInfo.put("filename", filename);
        audioInfo.put("url", url);

        databaseReference.child(key).setValue(audioInfo);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // super 호출 추가

        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 승인되었을 때
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // 권한이 거부되었을 때
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            mediaRecorder.release();
        }
    }
}

