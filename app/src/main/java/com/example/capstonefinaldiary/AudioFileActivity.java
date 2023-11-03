package com.example.capstonefinaldiary;


import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class AudioFileActivity extends AppCompatActivity {

    // 오디오 파일 재생 관련 변수
    private MediaPlayer mediaPlayer = null;
    private Boolean isPlaying = false;
    private Boolean isPaused = false;
    ImageView audioIcon;
    SeekBar seekBar;
    private TextView playTimeTextView; //진행시간
    private TextView totalTimeTextView; //완료시간 (오디오 파일 총 길이)
    private int lastPlayedPosition = -1;
    private ImageButton playImageBtn, stopImageBtn;

    /** 리사이클러뷰 */
    private RecyclerView audioRecyclerView;
    private AudioAdapter audioAdapter;
    private ArrayList<Uri> audioList;

    /** 검색창 */
    private SearchView searchView;
    /** 메뉴바 */
    //private MenuActivity menuActivity; // MenuActivity를 포함할 멤버 변수

    // 권한 요청 코드 (예: 1)
    private static final int PERMISSION_REQUEST_CODE = 21;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_file);
        //menuActivity = new MenuActivity(this); // BaseActivity 인스턴스 생성

        /**
         // AudioManager를 사용하여 오디오 스트림 설정
         AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
         audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_PLAY_SOUND);*/

        // 재생관련 버튼 초기화
        playImageBtn = findViewById(R.id.playImageBtn);
        stopImageBtn = findViewById(R.id.stopImageBtn);
        seekBar = findViewById(R.id.seekBar);
        playTimeTextView = findViewById(R.id.play_time_text_view);
        totalTimeTextView = findViewById(R.id.total_time_text_view);

        // 리사이클러뷰 초기화
        audioRecyclerView = findViewById(R.id.recyclerview);
        //getIntent()를 통해 녹음 파일 리스트 받기
        //audioList = getIntent().getParcelableArrayListExtra("recordedUris");
        /** 저장된 URI 목록을 불러옵니다 */
        audioList = loadRecordedUris();
        // Adapter에 데이터가 변경되었음을 알림
        audioAdapter = new AudioAdapter(this, audioList);
        audioRecyclerView.setAdapter(audioAdapter);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        audioRecyclerView.setLayoutManager(mLayoutManager);

        audioAdapter.notifyDataSetChanged();

        // 변수 초기화
        audioIcon = null;
        isPlaying = false;
        isPaused = false;
        lastPlayedPosition = -1;


        // 커스텀 이벤트 리스너 4. 액티비티에서 커스텀 리스너 객체 생성 및 전달
        audioAdapter.setOnItemClickListener(new AudioAdapter.OnIconClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String uriName = String.valueOf(audioList.get(position));
                File file = new File(uriName);

                if (position == lastPlayedPosition) {
                    // 같은 파일을 다시 클릭하면 아무 동작 안 함
                    return;
                }

                // 다른 파일을 클릭시
                if(isPlaying || isPaused) {
                    // 재생 중 또는 일시정지 중
                    stopAudio();

                }

                // 권한 요청
                if(requestStoragePermission()) {
                    audioIcon = (ImageView) view;
                    playAudio(file);
                    lastPlayedPosition = position;
                }
            }
        });

        playImageBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (isPaused) {
                    // 권한 요청
                    if(requestStoragePermission()) {
                        resumeAudio(); // 일시정지 상태에서 재개
                    }
                }
            }
        });
        stopImageBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (mediaPlayer.isPlaying()){
                    pauseAudio();
                }
            }
        });

    }
    // 녹음 파일 목록을 SharedPreferences에서 불러오는 함수
    private ArrayList<Uri> loadRecordedUris() {
        SharedPreferences preferences = getSharedPreferences("EDM", Context.MODE_PRIVATE);
        Set<String> uriStrings = preferences.getStringSet("recorded_uris", new HashSet<String>());

        // Set을 ArrayList로 변환
        ArrayList<Uri> recordedUris = new ArrayList<>();
        for (String uriString : uriStrings) {
            recordedUris.add(Uri.parse(uriString));
        }

        return recordedUris;
    }

    // 녹음 파일 재생
    private void playAudio(File file) {
        Log.d("AudioFileActivity", "playAudio() called"); // 디버깅을 위한 로그
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }else {
            mediaPlayer.reset();
        }

        // 오디오 속성 설정
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build();
        mediaPlayer.setAudioAttributes(audioAttributes);

        //mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    isPlaying = true;
                    isPaused = false;
                    // 재생관련 버튼 가시성 (UI)
                    playImageBtn.setVisibility(View.INVISIBLE);
                    stopImageBtn.setVisibility(View.VISIBLE);
                    updateSeekBar();
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // 재생이 완료된 경우 버튼 상태를 원래대로 복원
                    isPlaying = false;
                    isPaused = false;
                    playImageBtn.setVisibility(View.VISIBLE);
                    stopImageBtn.setVisibility(View.INVISIBLE);
                    resetSeekBar();
                }
            });
            mediaPlayer.prepareAsync(); // 비동기로 준비 시작

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("AudioFileActivity", "playAudio() not called"); // 디버깅을 위한 로그
        }
    }
    //녹음 재개
    private void resumeAudio() {
        // 일시정지 상태에서 재생버튼 클릭 시 재생 재개
        if (mediaPlayer != null && isPaused) {
            mediaPlayer.start();
            isPlaying = true;
            isPaused = false;
            playImageBtn.setVisibility(View.INVISIBLE);
            stopImageBtn.setVisibility(View.VISIBLE);
            Log.d("AudioFileActivity", "resumeAudio() called"); // 디버깅을 위한 로그
        }
    }

    // 녹음 파일 일시정지
    private  void pauseAudio(){
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            isPaused = true;
            playImageBtn.setVisibility(View.VISIBLE);
            stopImageBtn.setVisibility(View.INVISIBLE);
        }
    }


    // 녹음 파일 중지
    private void stopAudio() {
        isPlaying = false;
        isPaused = false;
        if (mediaPlayer != null &&(isPlaying||isPaused)) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    private void updateSeekBar() {
        if (mediaPlayer != null) {
            final int duration = mediaPlayer.getDuration();
            seekBar.setMax(duration);
            totalTimeTextView.setText(formatTime(duration));
            playTimeTextView.setText(formatTime(0));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isPlaying) {
                        try {
                            int currentPosition = mediaPlayer.getCurrentPosition();
                            seekBar.setProgress(currentPosition);
                            playTimeTextView.setText(formatTime(currentPosition));
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    private void resetSeekBar() {
        seekBar.setProgress(0);
        playTimeTextView.setText(formatTime(0));
        totalTimeTextView.setText(formatTime(0));
    }

    private String formatTime(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // 권한 요청 메서드
    private boolean requestStoragePermission() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // 사용자에게 권한에 대한 설명을 보여줄 필요가 있는 경우
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("권한 요청");
                builder.setMessage("오디오 파일을 읽기 위해서는 권한이 필요합니다. 권한을 부여해주세요.");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 사용자에게 권한 요청 다이얼로그를 표시
                        ActivityCompat.requestPermissions(AudioFileActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                    }
                });
                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 권한을 부여하지 않을 경우 처리
                        Toast.makeText(AudioFileActivity.this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.show();
            } else {
                // 권한 요청 다이얼로그를 표시
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
            return false;
        }
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티 종료 시 MediaPlayer 해제
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}