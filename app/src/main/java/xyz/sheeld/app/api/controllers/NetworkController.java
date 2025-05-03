package xyz.sheeld.app.api.controllers;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import xyz.sheeld.app.api.RetrofitClient;
import xyz.sheeld.app.api.dtos.GetNearestNodeResponseDTO;
import xyz.sheeld.app.api.interfaces.DataCallbackInterface;
import xyz.sheeld.app.api.routes.NetworkRoute;
import xyz.sheeld.app.api.types.Node;

public class NetworkController {
    private final NetworkRoute apiService = RetrofitClient.getClient().create(NetworkRoute.class);

    public void getsNodes(final DataCallbackInterface<List<Node>> callback) {
        Log.d("getNetworks", "enter");
        Call<List<GetNearestNodeResponseDTO>> call = apiService.getNodes();
        call.enqueue(
                new Callback<List<GetNearestNodeResponseDTO>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<GetNearestNodeResponseDTO>> call, @NonNull Response<List<GetNearestNodeResponseDTO>> response) {
                        if (response.isSuccessful() && response.code() == 200) {
                            if (response.body() != null) {
                                List<GetNearestNodeResponseDTO> data = response.body();
                                List<Node> nodeList = data.stream().map(n -> {
                                    Node node = new Node();
                                    node.ip = n.ip;
                                    node.networkPort = n.networkPort;
                                    node.apiPort = n.apiPort;
                                    node.joinedAt = n.joinedAt;
                                    return node;
                                }).collect(Collectors.toList());
                                callback.onSuccess(nodeList);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<GetNearestNodeResponseDTO>> call, @NonNull Throwable throwable) {
                        Log.e("getsNodes", throwable.toString());
                    }
                }
        );
    }

    public void getNearestNode(final DataCallbackInterface<Node> callback) {
        Call<GetNearestNodeResponseDTO> call = apiService.getNearestNode();
        call.enqueue(
                new Callback<GetNearestNodeResponseDTO>() {
                    @Override
                    public void onResponse(@NonNull Call<GetNearestNodeResponseDTO> call, @NonNull Response<GetNearestNodeResponseDTO> response) {
                        if (response.isSuccessful() && response.code() == 200) {
                            if (response.body() != null) {
                                GetNearestNodeResponseDTO data = response.body();
                                Node node = new Node();
                                node.ip = data.ip;
                                node.networkPort = data.networkPort;
                                node.apiPort = data.apiPort;
                                node.joinedAt = data.joinedAt;
                                callback.onSuccess(node);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<GetNearestNodeResponseDTO> call, @NonNull Throwable throwable) {
                        Log.e("getNearestNode", throwable.toString());
                    }
                }
        );
    }
}
