DocInfoFetcherSvc = ($http, $q, $log) ->
    deferred = $q.defer()

    $log.log('Spawning fetcher service')

    req_cb = (result) ->
        $log.log(result)
        deferred.notify(result)

    err_cb = (err) ->
        alert "Ajax error: #{err}"

    svc = {
        send_data: (aux_data) ->
            aux_data = aux_data ? {}
            aux_data['id'] = get_query_params().id
            $http.post('drive/doc', aux_data)
            .success(req_cb)
            .error(err_cb)

        on_new_data: (cb) ->
            deferred.promise.finally null, cb
    }

    svc.send_data()
    svc

DocInfoCtrl = ($scope, $sce, fetcher) ->
    $scope.back_link = '/'

    $scope.show_form = ->
        $scope.form_visible = true
    $scope.hide_form = ->
        $scope.form_visible = false

    fetcher.on_new_data (data) ->
        $scope.loaded = true
        $scope.doc = data
        $scope.tracked = data.base and data.halflife
        $scope.frame_link = $sce.trustAsResourceUrl(data.view_link)
        $scope.back_link = "/?id=#{data.parent}"
        $scope.hide_form()

FormCtrl = ($scope, fetcher) ->
    $scope.base = '50'
    $scope.half = '10000'
    $scope.submit_form = ->
        $scope.form_disabled = true
        fetcher.send_data {
            adjust_base: Number($scope.base)
            adjust_halflife: Number($scope.half)
        }

    fetcher.on_new_data (data) ->
        $scope.form_disabled = false
        if data.base
            $scope.base = data.base
        if data.halflife
            $scope.half = data.halflife

angular
.module('docViewModule', [])
.service('docInfoFetcher',
         ['$http', '$q', '$log', DocInfoFetcherSvc])
.controller('docInfoCtrl',
            ['$scope', '$sce', 'docInfoFetcher', DocInfoCtrl])
.controller('formCtrl',
            ['$scope', 'docInfoFetcher', FormCtrl])
