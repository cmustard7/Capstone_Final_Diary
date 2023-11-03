package com.example.capstonefinaldiary;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;

public class SettingActivity extends AppCompatActivity {

    private GoogleSignInClient mSignInClient;
    private FirebaseAuth mFirebaseAuth;
    private TextView logout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        logout = findViewById(R.id.logout);

        // logout 버튼 클릭 시 초기화
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mSignInClient.signOut();
                mFirebaseAuth.signOut();
                startActivity(new Intent(SettingActivity.this, MainActivity.class));
                finish();

            }
        });
    }
}