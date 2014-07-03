'use strict'

#define('angular', ['webjars!angular.js'], -> angular)

require([
  'angular',
  'angular-resource',
  'angular-route',
  '/users/javascripts/users-app.js'
], (angular) ->

  paws = angular.module('paws', ['ngResource', 'ngRoute', 'users'])

  paws.config [
    '$routeProvider',
    ($routeProvider) ->
      $routeProvider.when '/',
        templateUrl: 'common/partials/index.tpl.html'
      $routeProvider.otherwise
        redirectTo: '/'
  ]

  paws.factory 'navigation',
    ($resource) ->
      $resource('common/navigation')

  paws.directive 'pawsInput', ->
    restrict: 'E'
    replace: 'true'
    scope:
      name: '@'
      model: '='
      label: '@'
      placeholder: '@'
      size: '@'
      type: '@'
      value: '@'
      required: '@'
      errors: "="
    templateUrl: 'common/partials/input.tpl.html'

  paws.directive 'pawsForm', ->
    restrict: 'E'
    scope:
      controller: '='
      submit: '='

  paws.directive 'pawsSubmit', ->
    restrict: 'E'
    scope:
      class: '@'
      value: '@'
    templateUrl: 'common/partials/submit.tpl.html'

  paws.directive 'pawsButton', ->
    restrict: 'E'
    scope:
      click: '@'
      value: '@'
    templateUrl: 'common/partials/button.tpl.html'

  paws.directive 'pawsHeader', ->
    restrict: 'E'
    scope:
      h1: '@'
      h2: '@'
      lead: '@'
      subtext: '@'
      navModule: '@'
      navService: '@'
    templateUrl: 'common/partials/header.tpl.html'

  paws.directive 'pawsNav', ->
    restrict: 'E'
    scope:
      module: '@'
      service: '@'
    templateUrl: 'common/partials/navigation.tpl.html'
    link: ($scope) ->
      $scope.data = angular.injector([$scope.module]).get($scope.service).get(() -> $scope.$apply())

  angular.bootstrap(document, ['paws']);
)