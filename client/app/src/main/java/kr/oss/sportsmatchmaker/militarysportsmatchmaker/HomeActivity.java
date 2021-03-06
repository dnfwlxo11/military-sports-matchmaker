package kr.oss.sportsmatchmaker.militarysportsmatchmaker;

import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.*;

import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.ResponseHandlerInterface;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;


public class HomeActivity extends AppCompatActivity implements OnClickListener {

    //Helper
    private SessionManager smgr;
    private Proxy proxy;
    private HashMap<String, String> prof;
    private SimpleAdapter menuAdapter;
    private SwipeRefreshLayout mSwipeRefresh;

    // 위젯들 선언
    private TextView textWelcome;
    private Button logoutButton;
    private TextView textQStatus;
    private Button matching;
    private Button reserve;
    private Button viewGame;
    private Button edit;
    private ImageView homepro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         상태바 없애는 코드
         */
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_home);

        /*
         스와이프를 이용한 새로고침하기 위한 고트
         */
        mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override public void onRefresh() {
                Intent intent = new Intent(HomeActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                mSwipeRefresh.setRefreshing(false);
            }
        });

        smgr = new SessionManager(getApplicationContext());
        proxy = new Proxy(getApplicationContext());
        smgr.checkSession();

        // 위젯들 사용하기 위한 코드
        logoutButton = (Button) findViewById(R.id.logout);
        textWelcome = (TextView) findViewById(R.id.home_welcome);
        matching = (Button) findViewById(R.id.searchmatching);
        reserve = (Button) findViewById(R.id.reserveplace);
        reserve.setOnClickListener(this);

        viewGame = (Button) findViewById(R.id.viewGame);
        viewGame.setOnClickListener(this);
        edit = (Button) findViewById(R.id.profileedit);
        homepro = (ImageView) findViewById(R.id.homeprofile);


        matching.setOnClickListener(this);
        reserve.setOnClickListener(this);
        edit.setOnClickListener(this);
        homepro.setImageResource(R.drawable.img_defaultface);


        final String id = smgr.getProfile().get(SessionManager.ID);

        /*
         사용자 이미지를 불러오는데 없을경우 DefaultImage를 불러온다
         */
        updateTextWelcome();
        updateProfileImage();
        displayMatchStatus();

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                smgr.logout();
                finish();
            }
        });
        /*
         큐 상태 메세지
         */
        displayMatchStatus();
    }

    /*
     버튼 클릭 이벤트 선언
     */
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.searchmatching:
                if (smgr.getMatchStatus()){
                    Toast.makeText(getApplicationContext(), "현재 큐의 승낙상태를 확인합니다.", Toast.LENGTH_SHORT).show();
                    Intent intent4 = new Intent(getApplicationContext(), QueListActivity.class);
                    startActivity(intent4);
                    return;
                }
                Toast.makeText(getApplicationContext(), "종목을 고르시면 \n자동으로 팀원과 상대방을 찾아드립니다.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), ChooseSportActivity.class);
                startActivity(intent);
                break;
            case R.id.reserveplace:
                Toast.makeText(getApplicationContext(), "이미 사람을 다 모으셨나요? \n장소를 잡아드립니다.", Toast.LENGTH_SHORT).show();
                Intent intent1 = new Intent(getApplicationContext(), ReservePlaceActivity.class);
                startActivity(intent1);
                break;
            //프로필 수정
            case R.id.profileedit:
                Toast.makeText(getApplicationContext(), "개인 프로필 정보를 변경합니다.", Toast.LENGTH_SHORT).show();
                Intent intent3 = new Intent(getApplicationContext(), EditProfileActivity.class);
                startActivity(intent3);
                break;
            //장소 고르기
            case R.id.viewGame:
                if (viewGame.getText().equals("경기 정보")){
                    Intent intent5 = new Intent(getApplicationContext(), MatchCompleteActivity.class);
                    startActivity(intent5);
                }
                break;
            default:
                break;//임시 사진 선택
        }
    }

    /*
     매치상태에 따라 버튼의 내용를 바꾼다. 그리고 그에 대한 기능들도 바뀐다
     */
    private void displayMatchStatus(){
        viewGame.setBackgroundColor(getColor(android.R.color.darker_gray));
        viewGame.setText("경기 찾는 중...");
        textQStatus = (TextView) findViewById(R.id.home_qstatus);
        final String id = smgr.getProfile().get(SessionManager.ID);

        proxy.getUserInfo(new JsonHttpResponseHandler(){
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    final String match_status = response.getString("match_status");
                    // Case 1: 매치=큐가 없다=대기중이지 않다.
                    if (match_status.equals("ready")){
                        textQStatus.setText("현재 대기중인 시합이 없습니다. \n큐에 들어가보세요!");
                        smgr.changeMatchStatus(false, null, null);
                        smgr.changeStadiumName(null);
                        matching.setText("전투체육 같이 할 사람 찾기");
                        matching.setBackgroundColor(getColor(android.R.color.holo_blue_light));
                    }
                    // Case 2: 대기중인 매치가 있다. (수락 여부는 밑에서)
                    else {
                        matching.setText("큐 상태 보기");
                        int colorCode = Color.parseColor("#000099") ;
                        matching.setBackgroundColor(colorCode);
                        proxy.getUserMatch(new JsonHttpResponseHandler(){
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                try {
                                    boolean success = response.getBoolean("result");
                                    if (success) {
                                        JSONObject match = response.getJSONObject("match");
                                        // set match status and match id on session manager.
                                        smgr.changeMatchStatus(true, match.getString("matchId"), match.getString("activityType"));
                                        JSONObject stadium = match.getJSONObject("stadium");
                                        String stadium_name = stadium.getString("name");
                                        smgr.changeStadiumName(stadium_name);

                                        // Case 2의 코드 부분.
                                        JSONArray acceptPlayers = match.getJSONArray("players");
                                        JSONArray pendingPlayers = match.getJSONArray("pendingPlayers");
                                        int accnum = acceptPlayers.length();
                                        int pendnum = pendingPlayers.length();

                                        // Case 2-1. 매치 수락 대기중이다.
                                        if (match_status.equals("pending")){
                                            textQStatus.setText("큐 초대가 있습니다.\n큐 상태를 확인하고 수락/거절 여부를 선택해주세요.");
                                        }
                                        // Case 2-2. 매치를 수락했고 수락 대기인원이 있다.
                                        else if (pendnum > 0){
                                            textQStatus.setText(String.valueOf(accnum + pendnum) + "명 중 " + String.valueOf(pendnum) + "명이 수락 대기중입니다.\n큐 상태를 확인하세요.");
                                        }
                                        // Case 2-3. 매치를 수락했고 수락 대기인원이 없다 => 경기 찾는 중이다.
                                        else {
                                            textQStatus.setText("경기를 찾는 중입니다.\n큐 상태를 확인하세요.");
                                        }
                                        proxy.prepareMatchingTeamStadium(smgr.getStadiumName(), new JsonHttpResponseHandler() {
                                            @Override
                                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                                try {
                                                    boolean result = response.getBoolean("result");
                                                    // Case 3. 경기를 찾았다!!!
                                                    if (result) {
                                                        textQStatus.setText("경기를 찾았습니다!\n경기 정보를 확인하세요.");
                                                        viewGame.setBackgroundColor(getColor(android.R.color.holo_blue_dark));
                                                        viewGame.setText("경기 정보");
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });

                                    }
                                    else {
                                        Log.e("TAG", "DATA CORRUPT; match not ready but no match");
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Toast.makeText(getApplicationContext(), "데이터 오류입니다.", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                                textQStatus.setText("매치 정보를 가져오지 못했습니다. 다시 접속해주세요.");
                            }

                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /*
     다시 HomeActivity로 돌아온 경우
     */
    @Override
    protected void onResume() {
        super.onResume();
        smgr.checkSession();
        updateTextWelcome();
        updateProfileImage();
        displayMatchStatus();
    }


    /*
     HomeActivity 상단부분에 나오는 환영 문구
     */
    private void updateTextWelcome(){
        prof = smgr.getProfile();
        String user_name = prof.get(SessionManager.NAME);
        String user_rank = prof.get(SessionManager.RANK);
        textWelcome.setText("환영합니다, " + user_name + " " + user_rank + "님.\n오늘도 즐거운 하루 되세요!");
    }


    /*
     HomeActivity 상단쪽에 나오는 로그인한 회원의 프로필 사진 출력
     */
    private void updateProfileImage(){
        // get image and set it. if no image, set default image.
        proxy.getUserInfo(new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    boolean success = response.getBoolean("result");
                    if (success){
                        if (response.getBoolean("profile_image")){
                            proxy.getProfPic(response.getString("id"), new FileAsyncHttpResponseHandler(getApplicationContext()) {
                                public void onSuccess(int i, Header[] headers, File file){
                                    String filePath = file.getAbsolutePath();
                                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                                    RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                                    rbd.setCornerRadius(bitmap.getHeight()/8.0f);
                                    homepro.setImageDrawable(rbd);
                                }
                                @Override
                                public void onFailure(int i, Header[] headers, Throwable throwable, File file) {
                                    Log.e("TAG", "Error: file open failed");
                                }
                            });
                        }
                        else {
                            homepro.setImageResource(R.drawable.img_defaultface);
                        }
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "회원정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


}
