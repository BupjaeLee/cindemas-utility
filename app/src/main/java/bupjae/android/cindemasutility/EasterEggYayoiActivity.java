package bupjae.android.cindemasutility;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.androidquery.AQuery;


public class EasterEggYayoiActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_easter_egg_yayoi);

        AQuery aq = new AQuery(this);
        aq.id(R.id.image1).getImageView().setImageURI(Uri.parse("content://bupjae.android.cindemasutility.image/card/xl/1201501.webp"));
        aq.id(R.id.image2).getImageView().setImageURI(Uri.parse("content://bupjae.android.cindemasutility.image/card/xl/1301502.webp"));
        aq.id(R.id.image3).getImageView().setImageURI(Uri.parse("content://bupjae.android.cindemasutility.image/card/xl/1401010.webp"));
        aq.id(R.id.image4).getImageView().setImageURI(Uri.parse("content://bupjae.android.cindemasutility.image/card/xl/1501011.webp"));
    }

    public void onButtonClick(View view) {
        finish();
    }
}
