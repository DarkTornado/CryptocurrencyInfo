package com.darktornado.cryptocurrencyinfo

class CoinInfo(var name: String, var mark: String) : Comparable<CoinInfo?> {

    override fun compareTo(other: CoinInfo?): Int {
        return name.compareTo(other!!.name)
    }

}