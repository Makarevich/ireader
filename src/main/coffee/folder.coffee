angular
.module('folderModule', [])
.controller 'folderCtrl', ($scope, $http, $log) ->
    root = get_query_params().id ? 'root'
    $http.post('drive/folder', {folder_id:root})
    .error (err) ->
        alert "Ajax error: #{err}"
    .success (data) ->
        #$log.log(data)
        $scope.data = data
