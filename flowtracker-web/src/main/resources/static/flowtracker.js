(function(angular) {

'use strict';
angular.module('flowtracker', [])
    .controller('TrackersController', ['trackerLoader', '$scope', function(trackerLoader, $scope) {
      this.trackerData = trackerLoader.trackerData;
      $scope.selectedTracker = null;
      this.select = function(tracker) {
        console.log("selected: ", tracker);
        $scope.selectedTracker = tracker;
      }
    }])
    .factory('trackerLoader', ['$http', function($http) {
      var trackerData = {list: []};

      var refresh = function() {
        return $http.get('tracker').success(function(data) {
          trackerData.list = data;
        });
      };

      refresh();

      return {
        trackerData: trackerData
      };
    }])
    .controller('SettingsController', ['$http', function($http) {
      var c = this;
      this.settings = null;
      this.save = function() {
        console.log('Saving settings: ', c.settings);
        $http.post('settings', c.settings);
      };
      $http.get('settings').success(function(data) {
        console.log(data);
        c.settings = data;
      });
    }]);

})(window.angular);


