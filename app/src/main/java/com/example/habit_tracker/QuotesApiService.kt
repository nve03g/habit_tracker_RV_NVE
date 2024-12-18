import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// API-responsmodel
data class QuoteResponse(
    val quote: String,
    val author: String
)

// Retrofit API-interface
interface QuotesApiService {
    @GET("v1/quotes")
    fun getQuote(
        @Header("X-Api-Key") apiKey: String,
        @Query("category") category: String = "inspirational" // Optioneel
    ): Call<List<QuoteResponse>> // De API retourneert een lijst
}
