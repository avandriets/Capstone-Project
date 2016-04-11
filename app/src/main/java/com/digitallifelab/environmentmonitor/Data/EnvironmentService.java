package com.digitallifelab.environmentmonitor.Data;

import com.digitallifelab.environmentmonitor.Utils.Utility;
import java.util.HashMap;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface EnvironmentService {

    @Headers({"Content-Type: application/json"})
    @POST(Utility.PointsURL)
    Call<ResponseBody> postPoint(@Body PointsStore point, @Header("Authorization") String authorization);

    @Headers({"Content-Type: application/json"})
    @GET(Utility.PointsURL)
    Call<ResponseBody> getPoints(@Header("Authorization") String authorization);

    @Headers({"Content-Type: application/json"})
    @GET(Utility.PointsURLGetPoint)
    Call<ResponseBody> getPoint(@Path("id") long id, @Header("Authorization") String authorization);

    @Headers({"Content-Type: application/json"})
    @PUT(Utility.PointsURLEdit)
    Call<ResponseBody> editPoint(@Path("id") long id, @Body PointsStore point, @Header("Authorization") String authorization);

    @Headers({"Content-Type: application/json"})
    @DELETE(Utility.PicturesURLDelete)
    Call<ResponseBody> deletePoint(@Path("id") long id, @Header("Authorization") String authorization);

    @Multipart
    @POST(Utility.PicturesURL)
    Call<ResponseBody> postPointsPictures(@Part("pollution_mark") RequestBody id, @Part("full_photoURL\"; filename=\"image.jpg\" ") RequestBody photo, @Header("Authorization") String authorization);

    @Headers({"Content-Type: application/json"})
    @DELETE(Utility.PicturesURL + "{id}/")
    Call<ResponseBody> deletePicture(@Path("id") long id, @Header("Authorization") String authorization);

    @Headers({"Content-Type: application/json"})
    @POST(Utility.MessagesURL)
    Call<ResponseBody> postMessage(@Body MessagesStore message, @Header("Authorization") String authorization);

    @Headers({"Content-Type: application/json"})
    @POST(Utility.REGISTER_URL)
    Call<ResponseBody> registerDevice(@Body HashMap<String, String> body, @Header("Authorization") String authorization);

    @Headers({"Content-Type: application/json"})
    @POST(Utility.UNREGISTER_URL)
    Call<ResponseBody> unregisterDevice(@Body HashMap<String, String> body, @Header("Authorization") String authorization);
}