FROM local/c7-systemd
RUN yum install -y passwd
RUN yum install -y sudo
RUN yum install -y git
RUN yum install -y vim
RUN yum install -y curl
RUN yum install -y initscripts && yum clean all
RUN sudo su
RUN adduser teamartizan
RUN echo "artizan" |passwd --stdin teamartizan
RUN usermod -aG wheel teamartizan
RUN curl --silent --location https://rpm.nodesource.com/setup_6.x | bash -
RUN yum -y install nodejs
RUN mkdir -p /root/.ssh

ADD  teamartizan-lib-deploy /root/.ssh
RUN chmod 700 /root/.ssh/teamartizan-lib-deploy
RUN  echo "    IdentityFile /root/.ssh/teamartizan-lib-deploy" >> /etc/ssh/ssh_config
RUN  echo "git.coding.net,59.56.27.71 ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDHOWdwLpkos2CLli6DFvQ36yQE6Pe/PtFp3XwyirfZCIoGWnedaWI8zkJWVCs0wgOB9/urFepTDfV2wN49KGy1sl2/CCDEH2K/zeoEAZlTcBrhU17bwg1yMHCyJ7IM+zdLzItDEKYjgoWqVdUGK1dXQQlwt7GP4W7HqffelQQoVxOMoZ5N50MzD+nvV4y8iq0KwDQNy62iU4hui9ajCSVUDLu/06ucd5IojSI9keRIYAXvQf52TJ5EbvoBggp9RhjuWNEG8IhnPP6rzPS11Ocmwg/HsP8xOKL28AeDBAh6B6MEBDtlyp5Yfu9cwZJ9CFtU/x5fHFPtANmgIphAfwN1" >> /root/.ssh/known_hosts

WORKDIR /src
COPY . /src/
RUN yum install -y ourcareer-1.0.0-SNAPSHOT20170422155252.noarch.rpm
RUN yum clean all; systemctl enable ourcareer.service

CMD ["/usr/sbin/init"]
