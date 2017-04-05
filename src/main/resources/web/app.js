var app = angular.module("SwingIt", ['ui.router']);


app.config(function($stateProvider, $urlRouterProvider) {

    $urlRouterProvider.otherwise('/login');

    $stateProvider

        // HOME STATES AND NESTED VIEWS ========================================
        .state('login', {
            url: '/login',
            templateUrl: 'swingit/login.html',
            authenticate: false
          })
        .state('rounds', {
            url: '/rounds',
            templateUrl: 'swingit/rounds.html',
            authenticate: true,
            controller: 'RoundController'
        })
        .state('players', {
            url: '/players',
            templateUrl: 'swingit/players.html',
            authenticate: true,
            controller: 'PlayerController'
         })
        .state('leaderboard', {
            url: '/leaderboard',
            templateUrl: 'swingit/leaderboard.html',
            authenticate: true,
            controller: 'LeaderboardController'
         })
        // ABOUT PAGE AND MULTIPLE NAMED VIEWS =================================
        .state('logout', {
            url: '/logout',
            authenticate: false,
            controller: 'LogoutController'
        });



});

app.factory('authInterceptor', function ($rootScope, $q, $window) {
  return {
    request: function (config) {
      config.headers = config.headers || {};
      if ($window.sessionStorage.getItem('token')) {
        config.headers.Authorization = 'Bearer ' + $window.sessionStorage.getItem('token');
      }
      return config;
    },
    response: function (response) {
      if (response.status === 401) {
        // handle the case where the user is not authenticated
        $state.transitionTo("login");
      }
      return response || $q.when(response);
    }
  };
});

app.config(function ($httpProvider) {
  $httpProvider.interceptors.push('authInterceptor');
});



   app.controller('LoginController', function ($scope, $rootScope, $http, $window, $state) {
     $scope.credentials = {
       name: '',
       password: ''
     };
     $scope.errorMessage = '';
     $scope.login = function (credentials) {
       $http({
           method : "POST",
           url : "login",
           data : $scope.credentials,
           headers : {
               'Content-Type' : 'application/json'
           }
       }).then(function successCallback(response) {
            $window.sessionStorage.setItem('token', response.data.token);
            $window.sessionStorage.setItem('role', response.data.role);
            $window.sessionStorage.setItem('playerName', response.data.playerName);
            $window.sessionStorage.setItem('playerId', response.data.playerid);
            $rootScope.$broadcast('userLoggedIn');
            $state.transitionTo("leaderboard");
          }, function errorCallback(response) {
              $window.sessionStorage.removeItem('token');
              $scope.errorMessage='Invalid user name or password'
          });
     };
   });


   app.controller('LogoutController', function ($rootScope, $window, $state) {
        $window.sessionStorage.removeItem('token');
        $window.sessionStorage.removeItem('role');
        $window.sessionStorage.removeItem('playerName');
        $window.sessionStorage.removeItem('playerId');
        $rootScope.$broadcast('userLoggedOut');
        $state.transitionTo("login");
      });

    app.controller('IndexController', function ($scope, $window, $rootScope) {
            $scope.role = $window.sessionStorage.getItem('role');
            $rootScope.$on('userLoggedIn', function () {
                $scope.role = $window.sessionStorage.getItem('role');
            });
            $rootScope.$on('userLoggedOut', function () {
                $scope.role = "";
            });
          });


    //Controller Part
    app.controller("LeaderboardController", function($scope, $http, $state) {


        //Now load the data from server
        _refreshLeaderboard();

        function _refreshLeaderboard() {
            $http({
                method : 'GET',
                url : 'leaderboard'
            }).then(function successCallback(response) {
                $scope.boardentries = response.data;
            }, function errorCallback(response) {
                console.log(response.statusText);
                $state.transitionTo("login");
            });
        }


    });


    app.controller("RoundController", function($scope, $http, $state, $rootScope, $window) {


                $scope.rounds = [];
                $scope.roundForm = {
                    id : "",
                    date : "",
                    playerId : $window.sessionStorage.getItem('playerId'),
                    hcp : "",
                    score : ""
                };
                $scope.role = $window.sessionStorage.getItem('role');
                $scope.playerId = $window.sessionStorage.getItem('playerId');
                $scope.playerName = $window.sessionStorage.getItem('playerName');


                //Now load the data from server
                _refreshRoundData();
                if ($scope.role == 'ADMIN') {
                    _refreshPlayerData();
                }


                $scope.submitRound= function() {

                    var method = "";
                    var url = "";
                    if ($scope.roundForm.id == "") {
                        //Id is absent in form data, it is create new generator operation
                        method = "POST";
                        url = 'rounds';
                    } else {
                        //Id is present in form data, it is edit generator operation
                        method = "PUT";
                        url = 'rounds';
                    }

                    $http({
                        method : method,
                        url : url,
                        data : angular.toJson($scope.roundForm),
                        headers : {
                            'Content-Type' : 'application/json'
                        }
                    }).then( _success, _error );
                };

                $scope.deleteRound = function(round) {
                    $http({
                        method : 'DELETE',
                        url : 'rounds/' + round.id
                    }).then(_success, _error);
                };




                $scope.editRound = function(round) {
                    $scope.roundForm.id = round.id;
                    $scope.roundForm.date = new Date(round.date);
                    $scope.roundForm.playerId = round.playerId;
                    $scope.roundForm.hcp = round.hcp;
                    $scope.roundForm.score = round.score;

                };

                /* Private Methods */
                //HTTP GET- get all rounds collection
                function _refreshRoundData() {
                    $http({
                        method : 'GET',
                        url : 'rounds'
                    }).then(function successCallback(response) {
                        $scope.rounds = response.data.sort(function(a, b){
                                                          if(a.date < b.date) return -1;
                                                          if(a.date > b.date) return 1;
                                                          return 0;
                                                      });
                    }, function errorCallback(response) {
                        console.log(response.statusText);
                        _clearFormData();
                        $state.transitionTo("login");
                    });
                }

                //HTTP GET- get all rounds collection
                function _refreshPlayerData() {
                    $http({
                        method : 'GET',
                        url : 'players'
                    }).then(function successCallback(response) {
                        $scope.players = response.data;
                    }, function errorCallback(response) {
                        console.log(response.statusText);
                    });
                }



                function _success(response) {
                    _refreshRoundData();
                    _clearFormData()
                }

                function _error(response) {
                    console.log(response.statusText);
                }

                //Clear the form
                function _clearFormData() {
                    $scope.roundForm.id = "";
                    $scope.roundForm.date = "";
                    if ($rootScope.role == 'ADMIN') {
                        $scope.roundForm.playerId = null;
                    } else if ($rootScope.role == 'USER') {
                        $scope.roundForm.playerId = $rootScope.playerId;
                    }
                    $scope.roundForm.hcp = "";
                    $scope.roundForm.score = "";



                };
            });


            app.controller("PlayerController", function($scope, $http, $state, $window) {


                            $scope.players = [];
                            $scope.playerForm = {
                                id : "",
                                name : "",
                                username : "",
                                password : "",
                                role : "USER"
                            };
                            $scope.role = $window.sessionStorage.getItem('role');

                            //Now load the data from server
                            _refreshPlayerData();

                            $scope.submitPlayer= function() {

                                var method = "";
                                var url = "";
                                if ($scope.playerForm.id == "") {
                                    //Id is absent in form data, it is create new generator operation
                                    method = "POST";
                                    url = 'players';
                                } else {
                                    //Id is present in form data, it is edit generator operation
                                    method = "PUT";
                                    url = 'players';
                                }

                                $http({
                                    method : method,
                                    url : url,
                                    data : angular.toJson($scope.playerForm),
                                    headers : {
                                        'Content-Type' : 'application/json'
                                    }
                                }).then( _success, _error );
                            };

                            $scope.deletePlayer = function(player) {
                                $http({
                                    method : 'DELETE',
                                    url : 'players/' + player.id
                                }).then(_success, _error);
                            };


                            $scope.editPlayer = function(player) {
                                $scope.playerForm.id = player.id;
                                $scope.playerForm.name = player.name;
                                $scope.playerForm.username = player.username;
                                $scope.playerForm.password = player.password;
                                $scope.playerForm.role = player.role;
                            };



                            //HTTP GET- get all rounds collection
                            function _refreshPlayerData() {
                                $http({
                                    method : 'GET',
                                    url : 'players'
                                }).then(function successCallback(response) {
                                    $scope.players = response.data;
                                }, function errorCallback(response) {
                                    console.log(response.statusText);
                                    $state.transitionTo("login");
                                });
                            }



                            function _success(response) {
                                _refreshPlayerData();
                                _clearFormData()
                            }

                            function _error(response) {
                                console.log(response.statusText);
                            }

                            //Clear the form
                            function _clearFormData() {


                                $scope.playerForm.id = "";
                                $scope.playerForm.name = "";
                                $scope.playerForm.username = "";
                                $scope.playerForm.password = "";
                                $scope.playerForm.role = "USER";

                            };
                        });