package com.vaultvpn.network

import com.vaultvpn.data.model.*
import retrofit2.http.*

interface VpnApiService {

    @GET("vpn/servers")
    suspend fun getServers(): ApiResponse<ServersResponse>

    @GET("user/me")
    suspend fun getUser(): ApiResponse<User>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthTokens>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<AuthTokens>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: RefreshRequest): ApiResponse<AuthTokens>

    @POST("vpn/connect")
    suspend fun notifyConnect(@Body body: ConnectRequest): ApiResponse<WireguardConfig>

    @POST("vpn/disconnect")
    suspend fun notifyDisconnect(): ApiResponse<Unit>

    @GET("vpn/bridges")
    suspend fun getBridges(): ApiResponse<List<BridgeConfig>>
}

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val username: String, val email: String, val password: String)
data class RefreshRequest(val refreshToken: String)
data class ConnectRequest(val serverId: String, val bridgeType: String, val protocol: String)
