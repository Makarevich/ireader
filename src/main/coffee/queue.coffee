angular
.module('queueModule', [])
.controller 'queueCtrl', ($scope, $http, $log) ->
    $http.post('drive/queue')
    .error (err) ->
        alert "Ajax error: #{err}"
    .success (data) ->
        $log.log(data)
        $scope.data = data
