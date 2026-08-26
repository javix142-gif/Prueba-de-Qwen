package cl.javier.salaremote;

import android.Manifest;
import android.app.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.text.InputType;
import android.widget.*;
import cl.javier.salaremote.net.AndroidTvRemoteClient;
import cl.javier.salaremote.net.TvDiscovery;
import cl.javier.salaremote.ui.RemoteView;
import java.util.*;

public final class MainActivity extends Activity implements AndroidTvRemoteClient.Listener, TvDiscovery.Listener {
    private RemoteView remoteView; private AndroidTvRemoteClient remote; private TvDiscovery discovery;
    private AlertDialog deviceDialog; private LinearLayout deviceList; private TextView discoveryStatus;
    private final Set<String> discoveredHosts = new HashSet<>(); private String selectedHost; private String selectedName = "Conectar TV";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); getWindow().setStatusBarColor(0xff120f15); getWindow().setNavigationBarColor(0xff120f15);
        selectedHost=getPreferences(MODE_PRIVATE).getString("host",null); selectedName=getPreferences(MODE_PRIVATE).getString("name","Conectar TV");
        remote=new AndroidTvRemoteClient(this,this); discovery=new TvDiscovery(this,this);
        remoteView=new RemoteView(this,new RemoteView.Actions(){ public void onConnectTap(){showDevicePicker();} public void onKey(int keyCode){if(selectedHost==null)showDevicePicker();else remote.sendKey(keyCode);}});
        remoteView.setDevice(selectedName,false); setContentView(remoteView); if(selectedHost!=null)connectTo(selectedName,selectedHost);
    }
    private void showDevicePicker(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES},41);
        discoveredHosts.clear(); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); int p=dp(20); root.setPadding(p,p,p,p);
        discoveryStatus=new TextView(this); discoveryStatus.setText("Buscando TVs en tu Wi‑Fi…"); discoveryStatus.setTextSize(16); root.addView(discoveryStatus,new LinearLayout.LayoutParams(-1,-2));
        deviceList=new LinearLayout(this); deviceList.setOrientation(LinearLayout.VERTICAL); root.addView(deviceList,new LinearLayout.LayoutParams(-1,-2));
        Button manual=new Button(this); manual.setText("Ingresar IP manualmente"); manual.setOnClickListener(v->showManualIp()); root.addView(manual,new LinearLayout.LayoutParams(-1,-2));
        deviceDialog=new AlertDialog.Builder(this).setTitle("Conectar a Android / Google TV").setView(root).setNegativeButton("Cancelar",(d,w)->discovery.stop()).create(); deviceDialog.setOnDismissListener(d->discovery.stop()); deviceDialog.show(); discovery.start();
    }
    private void showManualIp(){ EditText input=new EditText(this); input.setHint("192.168.1.100"); input.setInputType(InputType.TYPE_CLASS_PHONE); int p=dp(20);input.setPadding(p,p,p,p);
        new AlertDialog.Builder(this).setTitle("IP de la televisión").setMessage("El teléfono y la TV deben estar en la misma red Wi‑Fi.").setView(input).setPositiveButton("Conectar",(d,w)->{String host=input.getText().toString().trim();if(!host.isEmpty()){if(deviceDialog!=null)deviceDialog.dismiss();connectTo("TV Kaicun",host);}}).setNegativeButton("Cancelar",null).show(); }
    private void connectTo(String name,String host){selectedHost=host;selectedName=name;remoteView.setDevice(name,false);getPreferences(MODE_PRIVATE).edit().putString("host",host).putString("name",name).apply();remote.connect(host,()->runOnUiThread(()->remote.startPairing(host,()->runOnUiThread(()->showPairingCode(host)))),()->runOnUiThread(()->remoteView.setDevice(selectedName,true)));}
    private void showPairingCode(String host){EditText input=new EditText(this);input.setHint("Ej.: A1B2C3");input.setSingleLine(true);input.setAllCaps(true);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Vincular TV").setMessage("Escribe el código de 6 caracteres que aparece en la pantalla de la televisión.").setView(input).setPositiveButton("Vincular",null).setNegativeButton("Cancelar",null).create();dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{String code=input.getText().toString().trim();if(!code.matches("(?i)[0-9a-f]{6}")){input.setError("Código de 6 caracteres (0-9 / A-F)");return;}dialog.dismiss();remote.finishPairing(code,host,()->runOnUiThread(()->connectTo(selectedName,host)));}));dialog.show();}
    @Override public void onDevice(String name,String host){runOnUiThread(()->{if(deviceList==null||discoveredHosts.contains(host))return;discoveredHosts.add(host);Button b=new Button(this);b.setAllCaps(false);b.setText(name+"\n"+host);b.setOnClickListener(v->{if(deviceDialog!=null)deviceDialog.dismiss();connectTo(name,host);});deviceList.addView(b,new LinearLayout.LayoutParams(-1,-2));});}
    @Override public void onState(String text){runOnUiThread(()->{if(discoveryStatus!=null)discoveryStatus.setText(text);});}
    @Override public void onStatus(String status,boolean connected){runOnUiThread(()->{if(connected)remoteView.setDevice(selectedName,true);if(!status.equals("Conectando…"))Toast.makeText(this,status,Toast.LENGTH_SHORT).show();});}
    @Override public void onError(String message){runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Sala Remote").setMessage(message).setPositiveButton("Aceptar",null).show());}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==41&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED&&deviceDialog!=null)discovery.start();}
    @Override protected void onDestroy(){discovery.stop();remote.disconnect();super.onDestroy();} private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
