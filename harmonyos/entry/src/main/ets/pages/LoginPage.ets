import router from '@ohos.router';
import { ApiClient } from '../services/ApiClient';

@Entry
@Component
struct LoginPage {
  @State username: string = '';
  @State password: string = '';
  @State realName: string = '';
  @State phone: string = '';
  @State gender: string = '男';
  @State age: string = '';
  @State isRegisterMode: boolean = false;
  @State loading: boolean = false;
  @State message: string = '';
  @State error: string = '';

  private api?: ApiClient;

  aboutToAppear(): void {
    this.api = new ApiClient(getContext(this));
    this.tryResumeSession();
  }

  async tryResumeSession(): Promise<void> {
    const session = await this.api?.getSession();
    if (session?.token && session.role.toLowerCase() === 'patient' && (!session.expiresAt || Date.now() < session.expiresAt)) {
      router.replaceUrl({ url: 'pages/RegistrationPage' });
    }
  }

  validate(): boolean {
    this.error = '';
    if (!this.username.trim()) {
      this.error = '请输入用户名';
      return false;
    }
    if (!this.password || this.password.length < 6) {
      this.error = '请输入至少 6 位密码';
      return false;
    }
    if (this.isRegisterMode) {
      if (!this.realName.trim()) {
        this.error = '请输入真实姓名';
        return false;
      }
      if (!/^1\d{10}$/.test(this.phone.trim())) {
        this.error = '请输入正确的手机号';
        return false;
      }
      if (this.age.trim()) {
        const ageNumber = Number(this.age);
        if (Number.isNaN(ageNumber) || ageNumber < 0 || ageNumber > 150) {
          this.error = '请输入有效年龄';
          return false;
        }
      }
    }
    return true;
  }

  async submit(): Promise<void> {
    if (!this.validate() || !this.api) {
      return;
    }

    this.loading = true;
    this.message = '';
    this.error = '';
    try {
      if (this.isRegisterMode) {
        await this.api.patientRegister({
          username: this.username.trim(),
          password: this.password,
          realName: this.realName.trim(),
          phone: this.phone.trim(),
          gender: this.gender,
          age: this.age.trim() ? Number(this.age) : null
        });
        this.message = '注册成功，请登录';
        this.isRegisterMode = false;
        return;
      }

      await this.api.patientLogin({
        username: this.username.trim(),
        password: this.password
      });
      router.replaceUrl({ url: 'pages/RegistrationPage' });
    } catch (err) {
      this.error = err instanceof Error ? err.message : '操作失败，请稍后重试';
    } finally {
      this.loading = false;
    }
  }

  build() {
    Column({ space: 18 }) {
      Column({ space: 6 }) {
        Text('HAVVK 智慧诊疗')
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
          .fontColor('#14213D')
        Text(this.isRegisterMode ? '患者注册' : '患者登录')
          .fontSize(15)
          .fontColor('#687385')
      }
      .alignItems(HorizontalAlign.Start)
      .width('100%')

      Row() {
        Button('登录')
          .type(ButtonType.Capsule)
          .backgroundColor(this.isRegisterMode ? '#EEF2F7' : '#0B7FEA')
          .fontColor(this.isRegisterMode ? '#526070' : '#FFFFFF')
          .layoutWeight(1)
          .onClick(() => {
            this.isRegisterMode = false;
            this.error = '';
            this.message = '';
          })
        Button('注册')
          .type(ButtonType.Capsule)
          .backgroundColor(this.isRegisterMode ? '#0B7FEA' : '#EEF2F7')
          .fontColor(this.isRegisterMode ? '#FFFFFF' : '#526070')
          .layoutWeight(1)
          .onClick(() => {
            this.isRegisterMode = true;
            this.error = '';
            this.message = '';
          })
      }
      .padding(4)
      .borderRadius(18)
      .backgroundColor('#EEF2F7')
      .width('100%')

      this.formInput('用户名', '请输入用户名', this.username, (value: string) => {
        this.username = value;
      })

      TextInput({ placeholder: '请输入密码', text: this.password })
        .type(InputType.Password)
        .height(48)
        .padding({ left: 14, right: 14 })
        .borderRadius(12)
        .backgroundColor('#F7F9FC')
        .onChange((value: string) => {
          this.password = value;
        })

      if (this.isRegisterMode) {
        this.formInput('真实姓名', '请输入真实姓名', this.realName, (value: string) => {
          this.realName = value;
        })
        this.formInput('手机号', '请输入手机号', this.phone, (value: string) => {
          this.phone = value;
        }, InputType.PhoneNumber)

        Row({ space: 10 }) {
          Select([
            { value: '男' },
            { value: '女' }
          ])
            .selected(this.gender === '女' ? 1 : 0)
            .value(this.gender)
            .height(48)
            .layoutWeight(1)
            .onSelect((index: number, value?: string) => {
              this.gender = value ?? (index === 1 ? '女' : '男');
            })

          TextInput({ placeholder: '年龄', text: this.age })
            .type(InputType.Number)
            .height(48)
            .padding({ left: 14, right: 14 })
            .borderRadius(12)
            .backgroundColor('#F7F9FC')
            .layoutWeight(1)
            .onChange((value: string) => {
              this.age = value;
            })
        }
      }

      if (this.message) {
        Text(this.message)
          .fontSize(13)
          .fontColor('#0F8A4B')
          .padding(10)
          .width('100%')
          .borderRadius(10)
          .backgroundColor('#EAF8F0')
      }

      if (this.error) {
        Text(this.error)
          .fontSize(13)
          .fontColor('#B42318')
          .padding(10)
          .width('100%')
          .borderRadius(10)
          .backgroundColor('#FEEDEB')
      }

      Button(this.loading ? '处理中...' : (this.isRegisterMode ? '注册' : '登录'))
        .height(50)
        .width('100%')
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
        .backgroundColor('#0B7FEA')
        .enabled(!this.loading)
        .onClick(() => {
          this.submit();
        })
    }
    .width('100%')
    .height('100%')
    .padding({ left: 22, right: 22, top: 70 })
    .backgroundColor('#FFFFFF')
  }

  @Builder
  formInput(
    label: string,
    placeholder: string,
    value: string,
    onChange: (value: string) => void,
    inputType: InputType = InputType.Normal
  ) {
    Column({ space: 6 }) {
      Text(label)
        .fontSize(13)
        .fontColor('#526070')
      TextInput({ placeholder, text: value })
        .type(inputType)
        .height(48)
        .padding({ left: 14, right: 14 })
        .borderRadius(12)
        .backgroundColor('#F7F9FC')
        .onChange(onChange)
    }
    .alignItems(HorizontalAlign.Start)
    .width('100%')
  }
}
