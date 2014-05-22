'use strict';

/* Services */

agate.factory('Account', ['$resource',
  function ($resource) {
    return $resource('ws/account', {}, {
    });
  }]);

agate.factory('Password', ['$resource',
  function ($resource) {
    return $resource('ws/account/change_password', {}, {
    });
  }]);

agate.factory('Sessions', ['$resource',
  function ($resource) {
    return $resource('ws/account/sessions/:series', {}, {
      'get': { method: 'GET', isArray: true}
    });
  }]);

agate.factory('MetricsService', ['$resource',
  function ($resource) {
    return $resource('metrics/metrics', {}, {
      'get': { method: 'GET'}
    });
  }]);

agate.factory('ThreadDumpService', ['$http',
  function ($http) {
    return {
      dump: function () {
        return $http.get('dump').then(function (response) {
          return response.data;
        });
      }
    };
  }]);

agate.factory('HealthCheckService', ['$rootScope', '$http',
  function ($rootScope, $http) {
    return {
      check: function () {
        return $http.get('health').then(function (response) {
          return response.data;
        });
      }
    };
  }]);

agate.factory('LogsService', ['$resource',
  function ($resource) {
    return $resource('ws/logs', {}, {
      'findAll': { method: 'GET', isArray: true},
      'changeLevel': { method: 'PUT'}
    });
  }]);

agate.factory('AuditsService', ['$http',
  function ($http) {
    return {
      findAll: function () {
        return $http.get('ws/audits/all').then(function (response) {
          return response.data;
        });
      },
      findByDates: function (fromDate, toDate) {
        return $http.get('ws/audits/byDates', {params: {fromDate: fromDate, toDate: toDate}}).then(function (response) {
          return response.data;
        });
      }
    }
  }]);

agate.factory('Session', ['$cookieStore',
  function ($cookieStore) {
    this.create = function (login, firstName, lastName, email, userRoles) {
      this.login = login;
      this.firstName = firstName;
      this.lastName = lastName;
      this.email = email;
      this.userRoles = userRoles;
    };
    this.destroy = function () {
      this.login = null;
      this.firstName = null;
      this.lastName = null;
      this.email = null;
      this.roles = null;
      $cookieStore.remove('account');
    };
    return this;
  }]);

agate.constant('USER_ROLES', {
  all: '*',
  admin: 'ROLE_ADMIN',
  user: 'ROLE_USER'
});

agate.factory('AuthenticationSharedService', ['$rootScope', '$http', '$cookieStore', 'authService', 'Session', 'Account',
  function ($rootScope, $http, $cookieStore, authService, Session, Account) {
    return {
      login: function (param) {
        var data = "username=" + param.username + "&password=" + param.password;
        $http.post('ws/auth/sessions', data, {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded"
          },
          ignoreAuthModule: 'ignoreAuthModule'
        }).success(function (data, status, headers, config) {
          Account.get(function (data) {
            Session.create(data.login, data.firstName, data.lastName, data.email, data.roles);
            $cookieStore.put('account', JSON.stringify(Session));
            authService.loginConfirmed(data);
          });
        }).error(function (data, status, headers, config) {
          Session.destroy();
        });
      },
      isAuthenticated: function () {
        if (!Session.login) {
          // check if the user has a cookie
          if ($cookieStore.get('account') != null) {
            var account = JSON.parse($cookieStore.get('account'));
            Session.create(account.login, account.firstName, account.lastName,
              account.email, account.userRoles);
            $rootScope.account = Session;
          }
        }
        return !!Session.login;
      },
      isAuthorized: function (authorizedRoles) {
        if (!angular.isArray(authorizedRoles)) {
          if (authorizedRoles == '*') {
            return true;
          }

          authorizedRoles = [authorizedRoles];
        }

        var isAuthorized = false;

        angular.forEach(authorizedRoles, function (authorizedRole) {
          var authorized = (!!Session.login &&
            Session.userRoles.indexOf(authorizedRole) !== -1);

          if (authorized || authorizedRole == '*') {
            isAuthorized = true;
          }
        });

        return isAuthorized;
      },
      logout: function () {
        $rootScope.authenticationError = false;
        $http.get('ws/logout')
          .success(function (data, status, headers, config) {
            Session.destroy();
            authService.loginCancelled();
          });
      }
    };
  }]);
