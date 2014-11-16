DocInfoFetcherSvc = ($http, $q, $log) ->
    q_new_data = $q.defer()
    q_ajax_status = $q.defer()

    req_cb = (result) ->
        $log.log(result)
        q_new_data.notify(result)
        q_ajax_status.notify(false)

    err_cb = (err) ->
        alert "Ajax error: #{err}"
        q_ajax_status.notify(false)

    svc =
        send_data: (aux_data) ->
            aux_data = aux_data ? {}
            aux_data['id'] = get_query_params().id
            $http.post('drive/doc', aux_data)
            .success(req_cb)
            .error(err_cb)
            q_ajax_status.notify(true)
        on_new_data: (cb) ->
            q_new_data.promise.finally null, cb
        on_ajax_busy: (cb) ->
            q_ajax_status.promise.finally null, cb

    svc.send_data()
    svc

DocInfoCtrl = ($scope, $sce, $window, fetcher) ->
    $scope.back_link = '/'

    $scope.read_the_doc = ->
        fetcher.send_data
            action: 'read'

    $scope.untrack = ->
        fetcher.send_data
            action: 'untrack'

    $scope.close_all_forms = ->
        $window.close_form_modal()

    fetcher.on_new_data (data) ->
        $scope.loaded = true
        $scope.doc = data
        $scope.tracked = data.base and data.half and data.ts
        $scope.frame_link = $sce.trustAsResourceUrl(data.view_link)
        $scope.back_link = "/?id=#{data.parent}"
        $scope.close_all_forms()

FormCtrl = ($scope, fetcher) ->
    $scope.base = '50'
    $scope.half = '10'
    $scope.submit_form = ->
        $scope.form_disabled = true
        fetcher.send_data
            action: if $scope.tracked then 'update' else 'init'
            base: Number($scope.base)
            half: Number($scope.half)

    fetcher.on_new_data (data) ->
        $scope.form_disabled = false
        if data.base
            $scope.base = data.base
        if data.half
            $scope.half = data.half

angular
.module('docViewModule', [])
.service('docInfoFetcher',
         ['$http', '$q', '$log', DocInfoFetcherSvc])
.controller('docInfoCtrl',
            ['$scope', '$sce', '$window', 'docInfoFetcher', DocInfoCtrl])
.controller('formCtrl',
            ['$scope', 'docInfoFetcher', FormCtrl])
