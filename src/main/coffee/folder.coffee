FolderCtrl = ($scope, $http, $log) ->
    root = get_query_params().id ? 'root'
    $http.post('drive/folder', {folder_id:root})
    .success (data) ->
        #$log.log(data)
        $scope.data = data
    .error (err) ->
        alert "Ajax error: #{err}"

angular
.module('folderModule', [])
.controller('folderCtrl', [ '$scope', '$http', '$log',
                            FolderCtrl ])
