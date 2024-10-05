package com.project.unitube.network.objectAPI;

import static com.project.unitube.utils.manager.UserManager.token;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.project.unitube.entities.User;
import com.project.unitube.network.RetroFit.RetrofitClient;
import com.project.unitube.network.interfaceAPI.UserWebServiceAPI;
import com.project.unitube.utils.manager.UserManager;

import java.io.File;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UserAPI {
    private MutableLiveData<User> currentUser = new MutableLiveData<>();

    Retrofit retrofit;
    UserWebServiceAPI userWebServiceAPI;

    public UserAPI( ) {
        retrofit = RetrofitClient.getClient();
        userWebServiceAPI = retrofit.create(UserWebServiceAPI.class);
    }


    public MutableLiveData<User> getUser(String username) {
        Call<User> call = userWebServiceAPI.getUser(username);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                new Thread(() -> {
                    currentUser.postValue(response.body());
                }).start();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
            }
        });
        return currentUser;
    }


    public MutableLiveData<User> loginUser(String username, String password) {
        Call<String> call = userWebServiceAPI.processLogin(username, password);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {

                if (response.isSuccessful()) {
                    // Assuming you receive a JSON object from the server with the token
                    String fullTokenResponse = response.body(); // This will be the whole JSON {"token":"<token_value>"}
                    String actualToken = fullTokenResponse.split(":")[1].replace("\"", "").replace("}", ""); // Extract just the token string

                    // Store the user locally using Room
                    UserManager.token = actualToken.trim();
                    currentUser = getUser(username);
                } else {
                    currentUser.postValue(null);  // Set null in case of failure
                }
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                currentUser.postValue(null);  // Set null in case of failure
            }
        });
        return currentUser;
    }


    public MutableLiveData<String> createUser(User user, Uri selectedPhotoUri) {
        MutableLiveData<String> resultLiveData = new MutableLiveData<>();

        // Create the parts for the request body (text fields)
        RequestBody firstNamePart = RequestBody.create(okhttp3.MultipartBody.FORM, user.getFirstName());
        RequestBody lastNamePart = RequestBody.create(okhttp3.MultipartBody.FORM, user.getLastName());
        RequestBody usernamePart = RequestBody.create(okhttp3.MultipartBody.FORM, user.getUserName());
        RequestBody passwordPart = RequestBody.create(okhttp3.MultipartBody.FORM, user.getPassword());

        // Create the file part for the profile picture if a photo was selected
        MultipartBody.Part profilePicturePart = null;
        if (selectedPhotoUri != null) {
            File file = new File(selectedPhotoUri.getPath());
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            profilePicturePart = MultipartBody.Part.createFormData("profilePicture", file.getName(), requestFile);
        }

        Call<Void> call = userWebServiceAPI.createUser(usernamePart, firstNamePart, lastNamePart, passwordPart, profilePicturePart);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    resultLiveData.postValue("success");
                } else if (response.code() == 409) {
                    resultLiveData.postValue("409");
                } else {
                    resultLiveData.postValue("failure");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                resultLiveData.postValue("failure");
            }
        });
        return resultLiveData;
    }

    public MutableLiveData<String> deleteUser(String userName) {
        MutableLiveData<String> resultLiveData = new MutableLiveData<>();

        Call<Void> call = userWebServiceAPI.deleteUser(userName);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    resultLiveData.postValue("success");
                } else if (response.code() == 403) {
                    resultLiveData.postValue("403");
                } else if (response.code() == 401) {
                    resultLiveData.postValue("401");
                } else {
                    resultLiveData.postValue("failure");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                resultLiveData.postValue("failure");
            }
        });
        return resultLiveData;
    }

    public MutableLiveData<String> updateUser(User user) {
        MutableLiveData<String> resultLiveData = new MutableLiveData<>();

        Call<Void> call = userWebServiceAPI.updateUser(user.getUserName(), user);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    resultLiveData.postValue("success");
                    UserManager.getInstance().setCurrentUser(user);
                } else if (response.code() == 403) {
                    resultLiveData.postValue("403");
                }
                else {
                    resultLiveData.postValue("failure");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("UserAPI", "Error updating user: " + t.getMessage());
            }
        });
        return  resultLiveData;
    }
}
