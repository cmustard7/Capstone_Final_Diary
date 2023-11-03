package com.example.capstonefinaldiary;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.capstonefinaldiary.Models.users;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private ImageView loginBtn;
    private GoogleSignInClient client;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private static final int RC_SIGN_IN = 100;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);         //activity_main.xml 연동

        loginBtn = findViewById(R.id.login_btn);
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://finalcapstone-749d2-default-rtdb.firebaseio.com/");

        // 로그인(id 토큰, 이메일)
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // client 초기화
        client = GoogleSignIn.getClient(this, options);
        client.revokeAccess();

        // 로그인 버튼 클릭 시
        loginBtn.setOnClickListener((View.OnClickListener) view -> {
            // Initialize sign in intent
            Intent intent = client.getSignInIntent();
            // Start activity for result
            startActivityForResult(intent, RC_SIGN_IN);
        });
    }

    // 로그인 결과를 보여 주는 메서드(로그인 성공 -> 계정 -> 파이어 베이스 인증)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 요청 코드가 같다면
        if(requestCode == RC_SIGN_IN)
        {
            // 로그인 성공 시 계정 가져 오기
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

                auth.signInWithCredential(credential).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // 인증 성공 시
                        if(task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();  // 현재 사용자 == 파이어 베이스 사용자
                            users users1 = new users();

                            // 사용자가 있다면
                            assert user != null;
                            // 데이터 가져오기
                            users1.setUserId(user.getUid());
                            users1.setUserName(user.getDisplayName());
                            users1.setProfilePic(user.getPhotoUrl().toString());
                            database.getReference().child("users").child(user.getUid()).setValue(users1);

                            // 로그인 성공 후 보여줄 데이터(이름, 사진)
                            Intent intent = new Intent(MainActivity.this, CalenderActivity.class);  // Main 이동
                            intent.putExtra("userId", user.getUid());
                            intent.putExtra("userName", user.getDisplayName());
                            intent.putExtra("ProfilePic", user.getPhotoUrl().toString());
                            startActivity(intent);
                        }
                        // 인증 실패 시
                        else {
                            Toast.makeText(MainActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();

                        }

                    }
                });

                // 로그인 실패 시
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
    }




}
