<?php

namespace App\Services;

use GuzzleHttp\Client;

class AuthService
{

    protected $client;
    protected $javaBaseUrl;

    /**
     * Create a new class instance.
     */
    public function __construct()
    {
        $this->client = new Client();
        $this->javaBaseUrl = rtrim(env('JAVA_API_URL', 'http://localhost:8080/'), '/') . '/api/v1/auth';
    }

    public function registerUser($userData) {
        $response = $this->client->post("{$this->javaBaseUrl}/register", [
            'json' => $userData
        ]);
        return json_decode($response->getBody()->getContents(), true);
    }
}
