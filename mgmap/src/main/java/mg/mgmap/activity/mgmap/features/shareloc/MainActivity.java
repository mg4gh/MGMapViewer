package mg.mgmap.activity.mgmap.features.shareloc;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import mg.mgmap.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_main);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        findViewById(R.id.btnRegister).setOnClickListener(v -> new Registration(this).show(getFilesDir()));
//        findViewById(R.id.btnLocationSettings).setOnClickListener(v -> new LocationSettingsDialog(this).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("Hello World - resume");
    }
}
