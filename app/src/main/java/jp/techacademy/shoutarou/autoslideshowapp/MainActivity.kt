package jp.techacademy.shoutarou.autoslideshowapp

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val PERMISSIONS_REQUEST_CODE = 100
    private var isGranted = false

    // 取得したURIを格納する配列
    private var listOfUris = arrayListOf<Uri>()

    //現在表示している画像のUriのインデックス
    private var currentUriIndex: Int = 0

    // スライドショーが再生中か否か
    private var isPlaySlideshow: Boolean = false

    // タイマー
    private var timer: Timer? = null

    //　タイマー操作オブジェクト
    private var handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // クリックリスナーの設定
        back_button.setOnClickListener(this)
        next_button.setOnClickListener(this)
        play_button.setOnClickListener(this)

        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                this.isGranted = true
                this.getContentsInfo()
                this.setImageOf()
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_CODE
                )
            }
            // Android 5系以下の場合
        } else {
            this.getContentsInfo()
            this.setImageOf()
        }
    }

    override fun onClick(v: View?) {

        // 許可の状態をチェック
        if (this.isGranted == false) {
            // Snackbarに許可を促すメッセージ
            Snackbar.make(v!!, "Appsの設定から画像の読み込みを許可してください", Snackbar.LENGTH_INDEFINITE)
                .setAction("わかりました"){
                    Log.d("UI_PARTS", "Snackbarをタップした")
                }.show()

            // メソッドを抜ける
            return
        }

        when (v?.id) {
            R.id.back_button -> {
                // デバッグプリント
                Log.d("ANDROID_DEBUGGER", "tapped back button")

                if (this.currentUriIndex <= 0) {
                    this.currentUriIndex = this.listOfUris.count() - 1
                } else {
                    this.currentUriIndex --
                }

                this.setImageOf(this.currentUriIndex)
            }
            R.id.next_button -> {
                // デバッグプリント
                Log.d("ANDROID_DEBUGGER", "tapped next button")

                if (this.currentUriIndex >= this.listOfUris.count() - 1) {
                    this.currentUriIndex = 0
                } else {
                    this.currentUriIndex ++
                }

                this.setImageOf(this.currentUriIndex)
            }
            R.id.play_button -> {
                // デバッグプリント
                Log.d("ANDROID_DEBUGGER","tapped play button.")

                //　ボタン表示の切り替え、送りボタンの制御、スライドショーの操作
                if (this.isPlaySlideshow) {
                    this.isPlaySlideshow = false

                    // ボタンUIの切り替え
                    play_button.text = "再生"
                    back_button.isClickable = true
                    next_button.isClickable = true
                    back_button.alpha = 1.toFloat()
                    next_button.alpha = 1.toFloat()

                    // スライドショー用のタイマーの停止、破棄
                    if (this.timer != null) {
                        this.timer!!.cancel()

                        // null にする
                        this.timer = null
                    }

                    Log.d("ANDROID_DEBUGGER", "Stop slideshow")
                } else {
                    this.isPlaySlideshow = true

                    //ボタンUIの切り替え
                    play_button.text = "停止"
                    back_button.isClickable = false
                    next_button.isClickable = false
                    back_button.alpha = 0.4.toFloat()
                    next_button.alpha = 0.4.toFloat()

                    /// 2秒に1回進むボタンを自動でクリックするタイマーの始動

                    // タイマーが存在しないことの確認
                    if (this.timer == null) {

                        // タイマーの生成
                        this.timer = Timer()

                        //　タイマーの始動
                        this.timer!!.schedule(object: TimerTask(){
                            override fun run() {

                                handler.post {
                                    next_button.performClick()
                                }
                            }
                        }, 2000, 2000)// 最初に始動させるまで 2000ミリ秒(2秒)、ループの間隔を 2000ミリ秒(2秒) に設定
                    }

                    Log.d("ANDROID_DEBUGGER", "Start slideshow")
                }
            }

        }// when

    }// onClick(:View?)

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
        ) {
            when (requestCode) {
                PERMISSIONS_REQUEST_CODE ->
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        // 許可を得た状態
                        this.isGranted = true

                        // 画像の情報を取得してリストに保管
                        this.getContentsInfo()
                        this.setImageOf()
                    } else {
                        Log.d("ANDROID_DEBUGGER","permission denied.")
                    }
            }
        }

        private fun getContentsInfo() {

            val resolver = contentResolver
            val cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
            )


            if (cursor!!.moveToFirst()) {
                do {
                    // indexからIDを取得し、そのIDから画像のURIを取得する
                    val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                    val id = cursor.getLong(fieldIndex)
                    val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    // デバッグプリント
                    Log.d("ANDROID", "URI : " + imageUri.toString())
                    Log.d("ANDROID", "${id}")
                    Log.d("ANDROID","${fieldIndex}")

                    // リストにUriを追加
                    this.listOfUris.add(imageUri)

                } while (cursor.moveToNext())

                // デバッグプリント
                Log.d("ANDROID","${this.listOfUris}")
            }

            cursor.close()
        }

    private fun setImageOf(uriIndex: Int = 0) {

        // image view に画像をセット
        imageView.setImageURI(this.listOfUris[uriIndex])

    }


    }
