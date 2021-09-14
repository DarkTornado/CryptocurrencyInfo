package com.darktornado.cryptocurrencyinfo

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.darktornado.library.SimpleRequester
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.util.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)

        val lay = LinearLayout(this)
        lay.orientation = 1
        lay.layoutParams = LinearLayout.LayoutParams(-1, -1)
        lay.gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
        val bar1 = ProgressBar(this)
        val pad: Int = dip2px(10)
        bar1.setPadding(pad, pad, pad, pad)
        lay.addView(bar1)
        val txt = TextView(this)
        txt.text = "암호화폐 목록 불러오는 중..."
        txt.setTextColor(Color.BLACK)
        txt.textSize = 14f
        txt.gravity = Gravity.CENTER
        lay.addView(txt)
        layout.addView(lay)

        setContentView(layout)
        Thread({ loadCoinList(layout, lay) }).start()
    }

    fun loadCoinList(layout: LinearLayout, lay: LinearLayout) {
        try {
            val list = ArrayList<CoinInfo>()
            val data = JSONArray(SimpleRequester.create("https://api.upbit.com/v1/market/all").execute().body)
            for (n in 0 until data.length()) {
                val datum: JSONObject = data.getJSONObject(n)
                val mark = datum.getString("market")
                if (!mark.startsWith("KRW-")) continue
                val coin = CoinInfo(datum.getString("korean_name"), mark)
                list.add(coin)
            }
            runOnUiThread {
                layout.removeView(lay)
                updateCoinList(layout, list.toTypedArray())
            }
        } catch (e: Exception) {
            toast("암호화폐 목록 불러오기 실패\n${e.toString()}")
        }
    }

    fun updateCoinList(layout: LinearLayout, coins: Array<CoinInfo>) {
        Arrays.sort(coins)
        val names = arrayOfNulls<String>(coins.size)
        for (n in coins.indices) {
            names[n] = coins[n].name;
        }
        val list = ListView(this)
        val adapter: ArrayAdapter<*> = ArrayAdapter<Any?>(this, android.R.layout.simple_list_item_1, names)
        list.adapter = adapter
        list.onItemClickListener = OnItemClickListener { parent: AdapterView<*>?, view: View?, pos: Int, id: Long ->
            Thread(Runnable { loadCoinInfo(coins[pos]) }).start()
        }
        layout.addView(list)
        val pad = dip2px(20)
        list.setPadding(pad, pad, pad, pad)
    }

    fun loadCoinInfo(coin: CoinInfo) {
        try {
            val url = "https://api.upbit.com/v1/ticker?markets=" + coin.mark
            val data = JSONArray(SimpleRequester.create(url).execute().body).getJSONObject(0)
            val df = DecimalFormat("###,###")
            val result = """
                현재 시세 : ${df.format(data.getLong("trade_price"))}원
                오늘 시가 : ${df.format(data.getLong("opening_price"))}원
                등락률 : ${if (data.getString("change") == "RISE") "+" else "-"}${Math.round(data.getDouble("change_rate") * 10000.0) / 100.0}%
                """.trimIndent()
            runOnUiThread {
                showDialog(coin.name + " (" + coin.mark.replace("KRW-", "") + ")", result)
            }
        } catch (e: java.lang.Exception) {
            toast(coin.name + " 정보 불러오기 실패\n" + e.toString())
        }
    }

    fun showDialog(title: String?, msg: CharSequence?) {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(title)
        dialog.setMessage(msg)
        dialog.setNegativeButton("닫기", null)
        dialog.show()
    }

    fun dip2px(dips: Int) = Math.ceil((dips * resources.displayMetrics.density).toDouble()).toInt()

    fun toast(msg: String?) = runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }

}