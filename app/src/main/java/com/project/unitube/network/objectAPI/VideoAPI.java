package com.project.unitube.network.objectAPI;

import androidx.lifecycle.MutableLiveData;

import com.project.unitube.entities.Video;
import com.project.unitube.Room.Dao.VideoDao;
import com.project.unitube.network.RetroFit.RetrofitClient;
import com.project.unitube.network.interfaceAPI.VideoWebServiceAPI;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class VideoAPI {
    private MutableLiveData<List<Video>> videoListData;
    private Video video;
    private VideoDao videoDao;
    VideoWebServiceAPI videoWebServiceAPI;

    public VideoAPI(MutableLiveData<List<Video>> VideoListData, VideoDao videoDao) {
        this.videoListData = VideoListData;
        this.videoDao = videoDao;
        Retrofit retrofit = RetrofitClient.getClient();
        videoWebServiceAPI = retrofit.create(VideoWebServiceAPI.class);
    }


    public MutableLiveData<List<Video>> getAllVideos() {
        Call<List<Video>> call = videoWebServiceAPI.getVideos();
        call.enqueue(new Callback<List<Video>>() {
            @Override
            public void onResponse(Call<List<Video>> call, Response<List<Video>> response) {
                if (response.isSuccessful() && response.body() != null) {
                new Thread(() -> {
                    videoDao.deleteAllVideos();
                    videoDao.insertAllVideos(response.body());
                    videoListData.postValue(videoDao.getAllVideos());
                }).start();
                }
            }

            @Override
            public void onFailure(Call<List<Video>> call, Throwable t) {
            }
        });
        return videoListData;
    }

    public MutableLiveData<Video> getVideoByID(int id) {
        MutableLiveData<Video> videoData = new MutableLiveData<>();
        Call<Video> call = videoWebServiceAPI.getVideoById(id);
        call.enqueue(new Callback<Video>() {
            @Override
            public void onResponse(Call<Video> call, Response<Video> response) {
                if (response.isSuccessful() && response.body() != null) {
                    videoData.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<Video> call, Throwable t) {
            }
        });
        return videoData;
    }














    public void getUserVideos(String userName) {
        Call<List<Video>> call = videoWebServiceAPI.getUserVideos(userName);
        call.enqueue(new Callback<List<Video>>() {
            @Override
            public void onResponse(Call<List<Video>> call, Response<List<Video>> response) {
                new Thread(() -> {
                    videoDao.deleteAllVideos();
                    videoDao.insertAllVideos(response.body());
                    videoListData.postValue(videoDao.getAllVideos());
                }).start();
            }

            @Override
            public void onFailure(Call<List<Video>> call, Throwable t) {
            }
        });
    }

    public void editVideo(String userName, int videoId, Video video) {
        Call<Void> call = videoWebServiceAPI.editVideo(userName, videoId, video);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                new Thread(() -> {
                    videoDao.updateVideo(video);
                    videoListData.postValue(videoDao.getAllVideos());
                }).start();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });
    }

    public void createVideo(Video video) {
        Call<Void> call = videoWebServiceAPI.createVideo(video);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                new Thread(() -> {
                    videoDao.insertVideo(video);
                    videoListData.postValue(videoDao.getAllVideos());
                }).start();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });
    }

    public void deleteVideo(String userName, int videoId) {
        Call<Void> call = videoWebServiceAPI.deleteVideo(userName, videoId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                new Thread(() -> {
                    videoDao.deleteVideo(videoDao.getVideoByID(videoId));
                    videoListData.postValue(videoDao.getAllVideos());
                }).start();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });
    }


}