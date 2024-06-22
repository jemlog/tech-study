### 쿠버네티스 클러스터 내부의 마이크로 서비스 간 제로 트러스트

- 클러스터 내부에서는 기본 설정으로 모든 파드가 네임스페이스 관계없이 서로 통신이 가능하다.
- 이 때 악의적인 사용자가 gateway 우회해서 직접 서비스 접근하거나 탈취할 수 있다
- 트래픽 암호화 하고 인증받은 사용자만 API 호출 가능하도록 만들어야 한다

클러스터 내부 트래픽은 모두 mTLS를 통해 패킷 암호화 하고, 확인된 서비스만 API 호출하도록 만들어줘야 한다.

Istio 서비스 메시 사용하면 mTLS 적용 가능하다

<img width="292" alt="스크린샷 2024-04-29 오후 4 40 24" src="https://github.com/CMC11th-Melly/Melly_Server/assets/82302520/833d6ca0-1eab-4983-93b9-f1e1c9cba729">

- 중간에 들어와서 패킷 캡쳐해도 내용 모른다
- API 호출하려고 해도 상호 인증 안됐기 때문에 호출 못한다.


보통 쿠버네티스 클러스터 외부에서 들어올때 ingress gateway를 거지고 거기서 1차적으로 클라이언트의 mTLS 체크한다. 그 다음 트래픽에 IStio Gateway가 X-Forwarded-Client-Cert 헤더에
인증서 정보와 함께 internal-secure-gateway로 넘긴다.

서로 통신할때 클라이언트는 자신의 인증서를 가지고 있다면 상대방은 클라이언트의 CA 인증서를 들고 있어야 한다.

근데 아직 제로 트러스트는 아니다. istio 서비스 메시 내의 서비스들은 모두 서로 호출 가능하다. 여기서 어떤 서비스가 어떤 서비스를 호출할 수 있는지 정확하게 인가 정책 필요하다.

만약 Istio 사이드카 사용하면 특정 워크로드에서 아웃바운드 트래픽 전송할때 도달할 수 있는 서비스 집합 조정한다.
인바운드에서 바인딩 되는 포트나 소켓을 조정할 수 있다.
egress config에 등록된 호스트로만 트래픽을 전달할 수 있다. egress host에 등록 필요.

기본적으로 isto는 도달 간으한 모든 서비스를 등록하지만, egress 스펙 가진 사이드카 리소스가 워크로드에 할당되면
egress 스펙에 해당하는 트래픽만 허용한다.

토스에서는 Envoy Proxy Access Log를 Elastic Search에 적재 중

AUthorizationPolicy 리소스를 사용해서 서버 단에서 접근 제어

마이데이터 사업에서 금융기관간 통신에는 mTLS 필수.

Serviceentry를 사용하면 외부서비스를 내부 서비스 메시로 포함시켜줄 수 있다.

토스는 계열사간의 통신에도 mTLS를 적용하고, 마이데이터 사업을 위한 외부와의 통신에도 mTLS 적용.

### 문제
- 보통 L7 프록시로 들어오면 클라이언트의 IP를 확인해서 X-Forwared-For 헤더에 넣어준다. 근데 mTLS 쓰면 Proxy에서는 아직 암호화 된 상태이기 때문에 헤더를 넣어줄 수가 없다.
- 만약 X-Forwared-For에서 상대방 누군지 안알려주면 내부 서비스 입장에서는 상대방이 L7 프록시이다.
    - Proxy Protocol을 사용해서 IP 정보 넣어준다. IP가 있어야 추가 보안 처리가 가능하다. ex. 유해 IP 차단
        - 외부사 IP를 Istio Gateway로 전달하면 Istio Gateway가 X-Forwared-For 헤더에 정보 넣어준다

### Kubernetes cluster의 mTLS를 사용한 Zero Trust 구현

Kubernetes cluster에서 Istio를 사용하면 Envoy Proxy를 사이드카로 파드와 함께 배포.
Envoy Proxy간 통신에는 자동으로 mTLS가 적용된다.
- **permissive mode** : Envoy Proxy로 plain text나 일반 tls 접근이 가능하도록 허용한다.
- **strict mode** : Envoy Proxy간 통신에서 양쪽 모두 mTLS를 통해 인증되는 경우에만 트래픽을 허용한다.

비대칭 키를 통해 서로의 인증서를 검증하고 암호화 한다
암호화 되지 않은 요청 거부
신뢰할 수 있는 클라이언트 인증서가 아닌 경우에도 거부