'use strict'

define(['angular'], (angular) ->

  users = angular.module('users', ['ngResource', 'ngRoute', 'ngTable'])

  users.config [
    '$routeProvider',
    ($routeProvider) ->
      $routeProvider.when '/home',
        templateUrl: 'users/partials/home.tpl.html'
      $routeProvider.when '/login',
        templateUrl: 'users/partials/login.tpl.html'
      $routeProvider.when '/signup',
        templateUrl: 'users/partials/signup-start.tpl.html'
      $routeProvider.when '/signup/:token',
        templateUrl: 'users/partials/signup.tpl.html'
      $routeProvider.when '/password/reset',
        templateUrl: 'users/partials/password-reset-start.tpl.html'
      $routeProvider.when '/password/reset/:token',
        templateUrl: 'users/partials/password-reset.tpl.html'
      $routeProvider.when '/password/change',
        templateUrl: 'users/partials/password-change.tpl.html'
      $routeProvider.otherwise
        redirectTo: '/home'
  ]

  users.factory 'navigation',
    ($resource) ->
      $resource('users/navigation')

  users.controller 'SignUpController',
    class SignUpController
      constructor: ($scope, $http, $location, $routeParams) ->
        $scope.form = {} if $scope.form is undefined

        $scope.generateName = ->
          $http.get('users/generate-name')
          .success (response) ->
              $scope.form.firstName = response.firstName
              $scope.form.lastName = response.lastName

        $scope.getEmail = ->
          $http.get('users/email/' + $routeParams.token)
          .success (response) ->
              $scope.form.email = response.email
              $location.path("login") if response.email = null

        $scope.sendEmail = ->
          $http.post('users/signup', $scope.form)
          .success () ->
              $location.path("login")
          .error (response) ->
              $scope.form.errors = response

        $scope.signUp = ->
          $http.post('users/signup/' + $routeParams.token, $scope.form)
          .success () ->
              $location.path("login")
          .error (response) ->
              $scope.form.errors = response

              if (response["password"])
                $scope.form.errors["password.password2"] = response["password"]

  users.controller 'LoginController',
    class LoginController
      constructor: ($scope, $http, $location) ->
        $scope.form = {} if $scope.form is undefined

        $scope.login = ->
          $http.post('users/authenticate/userpass', $scope.form)
          .success () ->
              $location.path("home")
          .error (response) ->
              $scope.form.errors = response

        $scope.logout = () ->
          $http.get('users/logout')
          .success(() -> $location.path("/login"))
          .error(() -> $location.path("/login"))

  users.controller 'PasswordController',
    class PasswordController
      constructor: ($scope, $http, $location, $routeParams) ->
        $scope.form = {} if $scope.form is undefined

        $scope.sendEmail = ->
          $http.post('users/password/reset', $scope.form)
          .success () ->
              $location.path("login")
          .error (response) ->
              $scope.form.errors = response

        $scope.reset = ->
          $http.post('users/password/reset/' + $routeParams.token, $scope.form)
          .success () ->
              $location.path("login")
          .error (response) ->
              $scope.form.errors = response

              if (response["password"])
                $scope.form.errors["password.password2"] = response["password"]

        $scope.change = ->
          $http.post('users/password/change')
          .success () ->
              $location.path("home")
          .error (response) ->
              $scope.form.errors = response

              if (response["password"])
                $scope.form.errors["password.password2"] = response["password"]
  users.controller 'TimeZoneAjaxController',
    class TimeZoneAjaxController
      constructor: ($scope, $timeout, $resource, ngTableParams) ->
        Zones = $resource('/users/homeJson');
        firstData =
          page: 1,            # show first page
          count: 10,          # count per page
          sorting:
            name: 'asc'     # initial sorting

        $scope.tableParams = new ngTableParams(firstData, {
          total: 0,           # length of data
          getData: ($defer, params) ->
            # ajax request to server
            Zones.get params.url(),
                    (data) ->
                      $timeout () ->
                        # update table params
                        params.total data.total
                        # set new data
                        $defer.resolve data.result
                      500
        })
        $scope.$on('reloadEvent', (event, args) -> $scope.tableParams.reload())

  users.controller 'TimeZoneAddController',
    class TimeZoneAddController
      constructor: ($scope, $http) ->
        $scope.form = {} if $scope.form is undefined

        $scope.addTimeZone = ->
          $http.post('users/timezone/add', $scope.form)
          .success () ->
              $scope.$emit('reloadEvent', {})
          .error (response) ->
              $scope.form.errors = response
)
