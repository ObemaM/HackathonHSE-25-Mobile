package com.example.hakaton;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SetUpActivity extends AppCompatActivity {

    private EditText editTextRegion, editTextSmc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        // Инициализация полей (код региона и код СМП)
        editTextRegion = findViewById(R.id.editTextRegion);
        editTextSmc = findViewById(R.id.editTextSmc);
        Button buttonSave = findViewById(R.id.buttonSave);

        buttonSave.setOnClickListener(v -> saveAndProceed());
    }

    private void saveAndProceed() {
        String region = editTextRegion.getText().toString().trim();
        String smc = editTextSmc.getText().toString().trim();

        // Проверка: оба поля обязательны
        if (region.isEmpty() || smc.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        // Сохраняем коды региона и СМП
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        prefs.edit()
                .putString("region_code", region)
                .putString("smc_code", smc)
                .putBoolean("is_first_run", false)
                .apply();

        // Переход в основной экран приложения
        Intent intent = new Intent(SetUpActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}