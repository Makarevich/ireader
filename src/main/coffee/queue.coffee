angular
.module('queueModule', [])
.controller 'queueCtrl', ($scope, $http, $log) ->
    $http.post('drive/queue')
    .error (err) ->
        $log.log(err)
        alert "Ajax error: #{err}"
    .success (data) ->
        $log.log(data)
        $scope.data = data
