package xyz.sheeld.app.api.controllers;

import android.util.Log;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import xyz.sheeld.app.api.RetrofitClient;
import xyz.sheeld.app.api.dtos.PostWalletOverviewResponseDTO;
import xyz.sheeld.app.api.dtos.PostWalletOverviewRequestDTO;
import xyz.sheeld.app.api.interfaces.DataCallbackInterface;
import xyz.sheeld.app.api.routes.WalletRoute;

public class WalletController {
    public void getWalletOverview(String address, final DataCallbackInterface<PostWalletOverviewResponseDTO> callback) {
        WalletRoute apiService = RetrofitClient.getClient().create(WalletRoute.class);

        PostWalletOverviewRequestDTO body = new PostWalletOverviewRequestDTO();
        body.address = address;

        Call<PostWalletOverviewResponseDTO> call = apiService.getWalletOverview(body);
        call.enqueue(
            new Callback<PostWalletOverviewResponseDTO>() {
                @Override
                public void onResponse(@NonNull Call<PostWalletOverviewResponseDTO> call, @NonNull Response<PostWalletOverviewResponseDTO> response) {
                    if (response.isSuccessful() && response.code() == 200) {
                        if (response.body() != null) {
                            PostWalletOverviewResponseDTO data = response.body();
                            callback.onSuccess(data);
                        }
                    } else if (response.code() == 404) {
                            callback.onFailure(new Error("No client found with the provided wallet address"));
                    } else {
                        String message = "Something went wrong while connecting to nearest node";
                        callback.onFailure(new Error(message));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<PostWalletOverviewResponseDTO> call, @NonNull Throwable throwable) {
                    Log.d("joinClient", throwable.toString());
                    callback.onFailure(throwable);
                }
            }
        );
    }
}
