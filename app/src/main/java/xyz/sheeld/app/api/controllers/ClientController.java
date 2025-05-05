package xyz.sheeld.app.api.controllers;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import xyz.sheeld.app.api.RetrofitClient;
import xyz.sheeld.app.api.dtos.PostClientJoinRequestDTO;
import xyz.sheeld.app.api.dtos.PostClientJoinResponseDTO;
import xyz.sheeld.app.api.interfaces.DataCallbackInterface;
import xyz.sheeld.app.api.routes.ClientRoute;

public class ClientController {
    public void joinClient(String baseurl, String ip, int networkPort, String sol_address, String signature, final DataCallbackInterface<Boolean> callback) {
        ClientRoute apiService = RetrofitClient.getDynamicClient(baseurl).create(ClientRoute.class);

        PostClientJoinRequestDTO body = new PostClientJoinRequestDTO();
        body.ip = ip;
        body.networkPort = networkPort;
        body.sol_address = sol_address;
        body.signature = signature;

        Call<PostClientJoinResponseDTO> call = apiService.joinClient(body);
        call.enqueue(
            new Callback<PostClientJoinResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<PostClientJoinResponseDTO> call, @NonNull Response<PostClientJoinResponseDTO> response) {
                    if (response.isSuccessful() && response.code() == 200) {
                        if (response.body() != null) {
                            PostClientJoinResponseDTO data = response.body();
                            callback.onSuccess(true);
                        }
                    } else {
                        String message = "Something went wrong while connecting to nearest node";
                        callback.onFailure(new Error(message));
                    };
                }

                @Override
                public void onFailure(@NonNull Call<PostClientJoinResponseDTO> call, @NonNull Throwable throwable) {
                    Log.d("joinClient", throwable.toString());
                    callback.onFailure(throwable);
                }
            }
        );
    }
}
