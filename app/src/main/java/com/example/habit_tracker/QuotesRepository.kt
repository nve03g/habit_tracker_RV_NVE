package com.example.habit_tracker.network

import QuoteResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QuotesRepository {
    fun fetchQuote(apiKey: String, onResult: (String) -> Unit) {
        val service = RetrofitInstance.api
        val call = service.getQuote(apiKey)

        call.enqueue(object : Callback<List<QuoteResponse>> {
            override fun onResponse(
                call: Call<List<QuoteResponse>>,
                response: Response<List<QuoteResponse>>
            ) {
                if (response.isSuccessful) {
                    val quoteResponse = response.body()
                    if (!quoteResponse.isNullOrEmpty()) {
                        val quote = "${quoteResponse[0].quote} - ${quoteResponse[0].author}"
                        onResult(quote)
                    } else {
                        onResult("Geen quote gevonden.")
                    }
                } else {
                    onResult("Fout: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<QuoteResponse>>, t: Throwable) {
                onResult("API-fout: ${t.message}")
            }
        })
    }
}
