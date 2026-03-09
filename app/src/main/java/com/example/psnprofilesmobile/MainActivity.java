package com.example.psnprofilesmobile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;


public class MainActivity extends AppCompatActivity {

    //Declaration a webview to interact with the webview element
    private WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //we set the content view to the layout we defined before in activity_main.xml
        setContentView(R.layout.activity_main);

        //find the WebView element by this id
        myWebView = findViewById(R.id.webView);

        //we get the websettings object to configure the webview setting
        WebSettings webSettings = myWebView.getSettings();

        //enable javascript
        webSettings.setJavaScriptEnabled(true);
        // we activate the dom storage
        webSettings.setDomStorageEnabled(true);

        // we activate the cookies
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        // we accept the thirdparty cookies
        cookieManager.setAcceptThirdPartyCookies(myWebView, true);


        //set webviewClient to ensure link open within the webView not the broser
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                //once we finished the page load, we inject the new css
                inyectCSS(view);
            }
        });

        myWebView.loadUrl("https://psnprofiles.com/guides");


    }

    // we give the punction the WebView object because we want to modify his content
    private void inyectCSS(WebView view) {
        try {
            // we open the inputStream so we can read the static resource located in the assets directory
            InputStream inputStream = getAssets().open("style_psn.css");

            //We wrap the InputStream ina  InputStreamReader in order to decode the bytes in to characters,
            // and use the BufferReader to read the text in blocks in order to improve the I/O performance
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder textConstructor = new StringBuilder();
            String line;

            //We iterate thru the data flow line by line and annex it to the buffer of the StringBuilder
            while ((line = reader.readLine()) != null) {
                textConstructor.append(line);
            }

            //We extract the content from the buffer as a immutable String
            String css = textConstructor.toString();

            //we close the in flow to release the memory and system resources
            inputStream.close();

            // We create a <meta> element to override the WebView default viewport behavior
            // 'width=device-width' forces the webpage rendering width to match the device's physical screen width
            // 'initial-scale=1.0, maximum-scale=1.0, user-scalable=no' locks the zoom level, giving it a native app feel
            String js = "var meta = document.createElement('meta');" +
                    "meta.name = 'viewport';" +
                    "meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';" +
                    "document.head.appendChild(meta);" +

                    // We create a <style> node, assign our parsed CSS string to it,
                    // and attach it to the <head> of the DOM.
                    "var style = document.createElement('style');" +
                    "style.innerHTML = '" + css + "';" +
                    "document.head.appendChild(style);";

            // we execute the JS script asynchronously int the WebView context
            view.evaluateJavascript(js, null);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
