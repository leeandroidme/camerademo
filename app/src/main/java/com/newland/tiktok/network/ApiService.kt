package com.newland.tiktok.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * @author: leellun
 * @data: 2021/6/7.
 *
 */
interface ApiService {
    @GET
    fun getImage(@Url url: String): retrofit2.Call<ResponseBody>?
}