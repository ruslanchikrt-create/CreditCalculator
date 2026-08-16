package com.example.creditcalculator;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

public class FeedbackActivity extends AppCompatActivity {
    public static final String EXTRA_KIND="feedback_kind";
    public static final String KIND_BUG="bug";
    public static final String KIND_IDEA="idea";
    private static final int REQ_FILES=4107;
    private static final int MAX_FILES=5;
    private static final long MAX_TOTAL_BYTES=100L*1024L*1024L;

    private final ArrayList<Uri> attachments=new ArrayList<>();
    private TextInputEditText titleInput,messageInput;
    private TextView attachmentInfo;
    private String kind;

    @Override protected void attachBaseContext(Context c){super.attachBaseContext(AppPreferences.wrapLocale(c));}
    @Override protected void onCreate(Bundle b){
        AppPreferences.applyNightMode(this);super.onCreate(b);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        kind=getIntent().getStringExtra(EXTRA_KIND);if(!KIND_IDEA.equals(kind))kind=KIND_BUG;
        setContentView(build());
    }

    private View build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);UiUtils.applyBackground(this,root);
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setBackgroundColor(ContextCompat.getColor(this,R.color.primary));root.addView(bar,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView back=top("‹",34);back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));
        TextView title=top(kindLabel(),20);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setGravity(Gravity.CENTER_VERTICAL);bar.addView(title,new LinearLayout.LayoutParams(0,-1,1f));

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(22),dp(20),dp(34));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));

        TextView intro=text(AppPreferences.tr(this,
                kind.equals(KIND_BUG)?"Опишите, что произошло и как повторить проблему.":"Расскажите, что вы хотите изменить или добавить в приложение.",
                kind.equals(KIND_BUG)?"Describe what happened and how to reproduce the problem.":"Tell us what you would like to change or add to the app."),15,R.color.text_secondary,false);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);ip.setMargins(0,0,0,dp(16));content.addView(intro,ip);

        titleInput=field(content,AppPreferences.tr(this,"Название обращения","Request title"),false);
        messageInput=field(content,AppPreferences.tr(this,"Описание","Description"),true);

        MaterialCardView attachCard=card();LinearLayout ab=box(attachCard);
        ab.addView(text(AppPreferences.tr(this,"Фото и видео","Photos and videos"),17,R.color.text_main,true));
        TextView note=text(AppPreferences.tr(this,"Можно прикрепить до 5 фото или видео, общим размером до 100 МБ. Приложение не копирует вложения в свою память и не сохраняет их после отправки.","You can attach up to 5 photos or videos, up to 100 MB total. The app does not copy attachments into its storage and does not keep them after sending."),13,R.color.text_secondary,false);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.setMargins(0,dp(5),0,dp(8));ab.addView(note,np);
        MaterialButton attach=outline(AppPreferences.tr(this,"Прикрепить фото или видео","Attach photos or videos"));attach.setOnClickListener(v->pickFiles());ab.addView(attach,new LinearLayout.LayoutParams(-1,dp(52)));
        attachmentInfo=text(AppPreferences.tr(this,"Вложения не выбраны","No attachments selected"),13,R.color.text_secondary,false);LinearLayout.LayoutParams aip=new LinearLayout.LayoutParams(-1,-2);aip.setMargins(0,dp(8),0,0);ab.addView(attachmentInfo,aip);
        LinearLayout.LayoutParams acp=new LinearLayout.LayoutParams(-1,-2);acp.setMargins(0,dp(16),0,dp(14));content.addView(attachCard,acp);

        MaterialButton send=new MaterialButton(this);send.setText(AppPreferences.tr(this,"Отправить обращение","Send request"));send.setAllCaps(false);send.setTextSize(16);send.setTextColor(Color.WHITE);send.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.primary)));send.setCornerRadius(dp(14));send.setOnClickListener(v->send());content.addView(send,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView privacy=text("ⓘ "+AppPreferences.tr(this,"В истории приложения останутся только название, дата, тип и текст обращения. Фото и видео не сохраняются.","Only the request title, date, type and text remain in app history. Photos and videos are not stored."),13,R.color.text_secondary,true);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.setMargins(0,dp(12),0,0);content.addView(privacy,pp);

        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime());v.setPadding(0,bars.top,0,0);scroll.setPadding(0,0,0,Math.max(bars.bottom,ime.bottom)+dp(18));UiUtils.ensureFocusedFieldVisible(scroll);return insets;});
        return root;
    }

    private TextInputEditText field(LinearLayout parent,String hint,boolean multiline){
        TextInputLayout l=new TextInputLayout(this);l.setHint(hint);l.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);l.setBoxBackgroundColor(ContextCompat.getColor(this,R.color.card_background));l.setBoxStrokeColor(ContextCompat.getColor(this,R.color.primary));l.setBoxCornerRadii(dp(12),dp(12),dp(12),dp(12));
        TextInputEditText e=new TextInputEditText(this);e.setTextColor(ContextCompat.getColor(this,R.color.text_main));if(multiline){e.setMinLines(5);e.setMaxLines(10);e.setGravity(Gravity.TOP|Gravity.START);}else e.setSingleLine(true);l.addView(e,new TextInputLayout.LayoutParams(-1,multiline?dp(150):dp(58)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(14));parent.addView(l,p);return e;
    }

    private void pickFiles(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","video/*"});i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        try{startActivityForResult(i,REQ_FILES);}catch(Exception e){Toast.makeText(this,AppPreferences.tr(this,"Не удалось открыть выбор файлов","Could not open file picker"),Toast.LENGTH_SHORT).show();}
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);if(requestCode!=REQ_FILES||resultCode!=RESULT_OK||data==null)return;
        ArrayList<Uri> picked=new ArrayList<>();ClipData clip=data.getClipData();if(clip!=null){for(int i=0;i<clip.getItemCount();i++)picked.add(clip.getItemAt(i).getUri());}else if(data.getData()!=null)picked.add(data.getData());
        long total=currentTotalBytes();int added=0;for(Uri u:picked){if(u==null||attachments.contains(u)||attachments.size()>=MAX_FILES)continue;String mime=getContentResolver().getType(u);if(mime!=null&&!mime.startsWith("image/")&&!mime.startsWith("video/"))continue;long size=fileSize(u);if(size>0&&total+size>MAX_TOTAL_BYTES){Toast.makeText(this,AppPreferences.tr(this,"Превышен общий лимит 100 МБ","The 100 MB total limit was exceeded"),Toast.LENGTH_LONG).show();break;}attachments.add(u);if(size>0)total+=size;added++;}
        if(added==0&&attachments.isEmpty())Toast.makeText(this,AppPreferences.tr(this,"Выберите фото или видео","Choose a photo or video"),Toast.LENGTH_SHORT).show();updateAttachmentInfo();
    }

    private void send(){
        String title=value(titleInput),message=value(messageInput);if(title.isEmpty()){titleInput.setError(AppPreferences.tr(this,"Укажите название","Enter a title"));titleInput.requestFocus();return;}if(message.isEmpty()){messageInput.setError(AppPreferences.tr(this,"Опишите обращение","Describe your request"));messageInput.requestFocus();return;}
        FeedbackStore.add(this,kindLabel(),title,message);
        String body=kindLabel()+"\n"+title+"\n\n"+message;
        Intent out;if(attachments.isEmpty()){out=new Intent(Intent.ACTION_SEND);out.setType("text/plain");}else{out=new Intent(Intent.ACTION_SEND_MULTIPLE);out.setType("*/*");out.putParcelableArrayListExtra(Intent.EXTRA_STREAM,new ArrayList<>(attachments));out.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);}out.putExtra(Intent.EXTRA_SUBJECT,title);out.putExtra(Intent.EXTRA_TEXT,body);
        try{startActivity(Intent.createChooser(out,AppPreferences.tr(this,"Отправить обращение","Send request")));Toast.makeText(this,AppPreferences.tr(this,"Текст обращения сохранён в истории","Request text saved in history"),Toast.LENGTH_SHORT).show();attachments.clear();updateAttachmentInfo();finish();}catch(Exception e){Toast.makeText(this,AppPreferences.tr(this,"Не найдено приложение для отправки. Текст обращения сохранён в истории.","No app was found for sending. The request text was saved in history."),Toast.LENGTH_LONG).show();attachments.clear();updateAttachmentInfo();}
    }

    private String kindLabel(){return AppPreferences.tr(this,KIND_IDEA.equals(kind)?"Предложение":"Ошибка",KIND_IDEA.equals(kind)?"Suggestion":"Bug");}
    private String value(TextInputEditText e){return e.getText()==null?"":e.getText().toString().trim();}
    private long currentTotalBytes(){long n=0;for(Uri u:attachments){long s=fileSize(u);if(s>0)n+=s;}return n;}
    private long fileSize(Uri u){try(Cursor c=getContentResolver().query(u,new String[]{OpenableColumns.SIZE},null,null,null)){if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.SIZE);if(i>=0&&!c.isNull(i))return c.getLong(i);}}catch(Exception ignored){}return 0;}
    private String fileName(Uri u){try(Cursor c=getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)return c.getString(i);}}catch(Exception ignored){}return AppPreferences.tr(this,"Файл","File");}
    private void updateAttachmentInfo(){if(attachments.isEmpty()){attachmentInfo.setText(AppPreferences.tr(this,"Вложения не выбраны","No attachments selected"));return;}StringBuilder s=new StringBuilder();for(int i=0;i<attachments.size();i++){if(i>0)s.append("\n");s.append("• ").append(fileName(attachments.get(i)));}attachmentInfo.setText(s.toString());}

    private MaterialCardView card(){MaterialCardView c=new MaterialCardView(this);c.setCardBackgroundColor(ContextCompat.getColor(this,R.color.card_background));c.setRadius(dp(18));c.setStrokeColor(ContextCompat.getColor(this,R.color.border));c.setStrokeWidth(dp(1));return c;}private LinearLayout box(MaterialCardView c){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(18),dp(16),dp(18),dp(16));c.addView(b);return b;}private MaterialButton outline(String s){MaterialButton b=new MaterialButton(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(ContextCompat.getColor(this,R.color.primary));b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.card_background)));b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.primary)));b.setStrokeWidth(dp(1));b.setCornerRadius(dp(14));return b;}private TextView top(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setClickable(true);return t;}private TextView text(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(ContextCompat.getColor(this,c));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
