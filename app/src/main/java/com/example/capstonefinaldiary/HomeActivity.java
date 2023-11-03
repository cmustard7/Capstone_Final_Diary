package com.example.capstonefinaldiary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

// 로그인 성공 후 화면
public class HomeActivity extends AppCompatActivity {

    private GoogleSignInClient mSignInClient;
    private FirebaseAuth mFirebaseAuth;

    ImageView ivProfile;
    TextView tv_Userid;
    TextView tv_Username;

    Button logoutBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Intent intent = getIntent();

        // MainActivity와 똑같이 입력해야 한다
        String userId = intent.getStringExtra("userId");
        String userName = getIntent().getStringExtra("userName");
        String ProfilePic = getIntent().getStringExtra("ProfilePic");

        ivProfile = findViewById(R.id.iv_Profile);
        tv_Userid = findViewById(R.id.tv_Userid);
        tv_Username = findViewById(R.id.tv_Username);
        logoutBtn = findViewById(R.id.logout_Btn);

        // 이미지 가져오기
        Glide.with(this)
                .load(ProfilePic)
                .circleCrop()
                .into(ivProfile);

        // 유저 계정
        tv_Userid.setText("Userid" + userId);

        // 유저 이름
        tv_Username.setText("UserName" + userName);

        // logout 버튼 클릭 시 초기화
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mSignInClient.signOut();
                mFirebaseAuth.signOut();
                startActivity(new Intent(HomeActivity.this, MainActivity.class));
                finish();

            }
        });

    }
}