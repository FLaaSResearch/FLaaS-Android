package org.sensingkit.flaas.network;

import org.sensingkit.flaas.model.RetroProject;
import org.sensingkit.flaas.model.RetroRound;
import org.sensingkit.flaas.model.RetroToken;

import java.util.List;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface APIService {

    String URL = "https://flaas.sensingkit.org";  // Production

    @GET("/api/project")
    Call<List<RetroProject>> getAllProjects();

    @GET("/api/project/{projectId}")
    Call<RetroProject> getProject(
            @Path("projectId") Integer projectId);

    //@Streaming (TODO: check http://www.programmersought.com/article/3643864508/)
    @GET("/api/get-samples/{dataset_type}/{app}/")
    Call<ResponseBody> getSamples(
            @Path("dataset_type") String datasetType,
            @Path("app") Integer app
    );

    //@Streaming (TODO: check http://www.programmersought.com/article/3643864508/)
    @GET("/api/project/{projectId}/get-model/{round}/")
    Call<ResponseBody> downloadModel(
            @Path("projectId") Integer projectId,
            @Path("round") Integer round
    );

    @GET("/api/project/{projectId}/join-round/{round}/")
    Call<RetroRound> joinRound(
            @Path("projectId") Integer projectId,
            @Path("round") Integer round,
            @Query("status") Integer status
    );

    @Headers("Content-Type: application/json")
    @POST("/api/report-availability")
    Call<ResponseBody> reportAvailability(
            @Body RequestBody body
    );

    @POST("/api/project/{projectId}/submit-results/{round}/{filename}")
    Call<ResponseBody> submitResults(
            @Path("projectId") Integer projectId,
            @Path("round") Integer round,
            @Path("filename") String filename,
            @Body RequestBody  file
    );

    @POST("/api/project/{projectId}/submit-model/{round}/{filename}")
    Call<ResponseBody> submitModel(
            @Path("projectId") Integer projectId,
            @Path("round") Integer round,
            @Path("filename") String filename,
            @Body RequestBody  file
    );

    // JWT Authentication
    @FormUrlEncoded
    @POST("/api/token/")
    Call<RetroToken> authenticate(
            @Field("username") String username,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("/api/token/refresh/")
    Call<RetroToken> refreshToken(
            @Field("refresh") String refresh
    );
}
